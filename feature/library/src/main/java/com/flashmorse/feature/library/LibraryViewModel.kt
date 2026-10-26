package com.flashmorse.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.data.repository.HistoryRepository
import com.flashmorse.core.data.repository.PhraseRepository
import com.flashmorse.core.model.Phrase
import com.flashmorse.core.model.activeScript
import com.flashmorse.core.model.isActive
import com.flashmorse.core.morse.codec.MorseEncoder
import com.flashmorse.core.morse.timing.ScriptCompiler
import com.flashmorse.core.torch.MorseTransmitter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 短语库与历史（F-09 / F-10）。 */
class LibraryViewModel(
    private val phraseRepository: PhraseRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val transmitter: MorseTransmitter,
) : ViewModel() {

    private val encoder = MorseEncoder()

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var wpm = 10
    private var compensation = 0

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                wpm = settings.wpm
                compensation = settings.compensationMillis
                _state.update { it.copy(wpm = settings.wpm) }
            }
        }

        viewModelScope.launch {
            phraseRepository.load()
            phraseRepository.phrases.collect { phrases ->
                _state.update { current ->
                    current.copy(phrases = phrases.map { it.toItem() })
                }
            }
        }

        viewModelScope.launch {
            historyRepository.load()
            historyRepository.entries.collect { entries ->
                _state.update { it.copy(history = entries) }
            }
        }

        viewModelScope.launch {
            transmitter.state.collect { playback ->
                _state.update {
                    it.copy(
                        isPlaying = playback.isActive,
                        playingText = playback.activeScript?.code?.plainText?.ifBlank { null },
                    )
                }
            }
        }
    }

    fun onTabSelected(index: Int) {
        _state.update { it.copy(tab = if (index == 0) LibraryTab.Phrases else LibraryTab.History) }
    }

    /** 列表项的主操作：正在发送则停止，否则立即发送该条。 */
    fun onPlay(payload: String) {
        if (_state.value.isPlaying) {
            transmitter.stop()
            return
        }
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return
        val timing = com.flashmorse.core.model.TimingProfile.forWpm(wpm, compensation)
        val script = ScriptCompiler.compile(encoder.encode(trimmed), timing)
        if (script.isEmpty) {
            _messages.trySend("该条目无法编码")
            return
        }
        val channel = transmitter.resolveChannel(
            com.flashmorse.core.model.ChannelPreference.Auto,
            timing,
        )
        transmitter.play(script, loop = false, channel = channel)
        viewModelScope.launch { historyRepository.record(trimmed, wpm, channel) }
    }

    fun onStop() = transmitter.stop()

    fun onEditorOpen(phrase: Phrase? = null) {
        // 先收起长按菜单，等它的退场动画结束再开编辑框：
        // 同一帧内「移除一个 OverlayDialog + 添加另一个」时，popup 宿主的注销与注册
        // 会挤在同一帧，容易让新弹层的入场动画从上一层的位移中途开始，出现闪烁。
        _state.update { it.copy(menuTarget = null) }
        viewModelScope.launch {
            delay(DIALOG_HANDOFF_MILLIS)
            _state.update {
                it.copy(
                    editor = phrase?.let { p ->
                        PhraseEditor(id = p.id, label = p.label, payload = p.payload)
                    } ?: PhraseEditor(),
                )
            }
        }
    }

    fun onEditorLabelChange(value: String) {
        _state.update { it.copy(editor = it.editor?.copy(label = value)) }
    }

    fun onEditorPayloadChange(value: String) {
        _state.update { it.copy(editor = it.editor?.copy(payload = value)) }
    }

    fun onEditorSave() {
        val editor = _state.value.editor ?: return
        if (!editor.canSave) {
            _messages.trySend("请填写报文内容")
            return
        }
        viewModelScope.launch {
            if (editor.id == null) {
                phraseRepository.add(editor.label, editor.payload)
            } else {
                phraseRepository.update(editor.id, editor.label, editor.payload)
            }
            _state.update { it.copy(editor = null) }
        }
    }

    fun onEditorDismiss() {
        _state.update { it.copy(editor = null) }
    }

    fun onMenuOpen(phrase: Phrase) {
        _state.update { it.copy(menuTarget = phrase) }
    }

    fun onMenuDismiss() {
        _state.update { it.copy(menuTarget = null) }
    }

    fun onDelete(phrase: Phrase) {
        viewModelScope.launch {
            phraseRepository.delete(phrase.id)
            _state.update { it.copy(menuTarget = null) }
        }
    }

    fun onMove(phrase: Phrase, offset: Int) {
        viewModelScope.launch {
            phraseRepository.move(phrase.id, offset)
            _state.update { it.copy(menuTarget = null) }
        }
    }

    fun onHistoryDelete(entryId: String) {
        viewModelScope.launch { historyRepository.delete(entryId) }
    }

    fun onHistoryClear() {
        viewModelScope.launch { historyRepository.clear() }
    }

    private fun Phrase.toItem(): PhraseItem {
        val pattern = encoder.encode(payload).letters.firstOrNull()?.pattern ?: ""
        return PhraseItem(phrase = this, previewPattern = pattern)
    }

    companion object {
        private const val DIALOG_HANDOFF_MILLIS = 180L

        fun factory(
            phraseRepository: PhraseRepository,
            historyRepository: HistoryRepository,
            settingsRepository: SettingsRepository,
            transmitter: MorseTransmitter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LibraryViewModel(phraseRepository, historyRepository, settingsRepository, transmitter)
            }
        }
    }
}
