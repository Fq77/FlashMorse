package com.flashmorse.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.MorseLetter
import com.flashmorse.core.model.isActive
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.morse.codec.MorseTable
import com.flashmorse.core.morse.timing.ScriptCompiler
import com.flashmorse.core.torch.MorseTransmitter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 摩斯对照表（F-14）。 */
class LearnViewModel(
    private val settingsRepository: SettingsRepository,
    private val transmitter: MorseTransmitter,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LearnUiState(
            alphabet = MorseTable.alphabet,
            digits = MorseTable.digits,
            punctuation = MorseTable.punctuation,
            selected = MorseTable.alphabet.firstOrNull(),
        ),
    )
    val state: StateFlow<LearnUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var wpm = TimingProfile.DEFAULT_WPM
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
            transmitter.state.collect { playback ->
                val playingChar = if (playback.isActive) _state.value.playingChar else null
                if (playingChar != _state.value.playingChar) {
                    _state.update { it.copy(playingChar = playingChar) }
                }
            }
        }
    }

    fun onTabSelected(index: Int) {
        val tab = when (index) {
            0 -> LearnTab.Letters
            1 -> LearnTab.Digits
            else -> LearnTab.Punctuation
        }
        _state.update { current ->
            current.copy(
                tab = tab,
                selected = when (tab) {
                    LearnTab.Letters -> current.alphabet.firstOrNull()
                    LearnTab.Digits -> current.digits.firstOrNull()
                    LearnTab.Punctuation -> current.punctuation.firstOrNull()
                },
            )
        }
    }

    fun onSelect(letter: MorseLetter) {
        _state.update { it.copy(selected = letter) }
    }

    /** 试闪：按当前速度发送单个字符。 */
    fun onPreview(letter: MorseLetter) {
        if (_state.value.playingChar != null) {
            transmitter.stop()
            _state.update { it.copy(playingChar = null) }
            return
        }
        val timing = TimingProfile.forWpm(wpm, compensation)
        val script = ScriptCompiler.compileSingle(letter.char, letter.pattern, timing)
        val channel = transmitter.resolveChannel(ChannelPreference.Auto, timing)
        _state.update { it.copy(selected = letter, playingChar = letter.char) }
        transmitter.play(script, loop = false, channel = channel)
    }

    fun onStop() {
        transmitter.stop()
        _state.update { it.copy(playingChar = null) }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            transmitter: MorseTransmitter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { LearnViewModel(settingsRepository, transmitter) }
        }
    }
}
