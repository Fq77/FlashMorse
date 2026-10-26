package com.flashmorse.feature.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.data.repository.HistoryRepository
import com.flashmorse.core.model.AbortReason
import com.flashmorse.core.model.PlaybackState
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.morse.codec.MorseEncoder
import com.flashmorse.core.morse.codec.TextNormalizer
import com.flashmorse.core.morse.timing.ScriptCompiler
import com.flashmorse.core.torch.MorseTransmitter
import com.flashmorse.core.torch.ThermalGuard
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 发报页状态生产。
 *
 * 只做三件事：把用户输入编译成 [com.flashmorse.core.model.SendScript]、
 * 把播放指令转给调度器、把调度器状态映射成 UI 状态。
 */
class SenderViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
    private val transmitter: MorseTransmitter,
    private val thermalGuard: ThermalGuard,
) : ViewModel() {

    private val encoder = MorseEncoder()

    private val _state = MutableStateFlow(SenderUiState())
    val state: StateFlow<SenderUiState> = _state.asStateFlow()

    /** 一次性提示（Snackbar）。 */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var messageInitialized = false
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        wpm = settings.wpm,
                        timing = settings.timingProfile,
                        channelPreference = settings.channel,
                        hapticFeedback = settings.hapticFeedback,
                        loopByDefault = settings.loopByDefault,
                        sosDisclaimerAccepted = settings.sosDisclaimerAccepted,
                    )
                }
                if (!messageInitialized) {
                    messageInitialized = true
                    applyMessage(settings.lastMessage, persist = false)
                }
                recompile()
            }
        }

        viewModelScope.launch {
            transmitter.state.collect { playback ->
                _state.update {
                    it.copy(
                        playback = playback,
                        constantLightOn = transmitter.isConstantLightOn(),
                    )
                }
                when (playback) {
                    is PlaybackState.Aborted -> {
                        _messages.trySend(abortMessage(playback.reason))
                        transmitter.consumeTerminalState()
                    }

                    is PlaybackState.Completed -> {
                        viewModelScope.launch {
                            delay(COMPLETED_DISPLAY_MILLIS)
                            transmitter.consumeTerminalState()
                        }
                    }

                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            thermalGuard.level.collect { level ->
                _state.update { it.copy(thermalLevel = level) }
            }
        }
    }

    fun onMessageChange(text: String) {
        if (text.length > MAX_LENGTH) return
        if (_state.value.isSending) return
        applyMessage(text, persist = true)
    }

    /** 主发送卡的唯一动作：空闲时发送，发送中停止。 */
    fun onPrimaryAction() {
        if (_state.value.isSending) {
            transmitter.stop()
        } else {
            sendMessage(loop = _state.value.loopByDefault)
        }
    }

    fun onSosClick() {
        if (_state.value.isSending) {
            transmitter.stop()
            return
        }
        if (!_state.value.sosDisclaimerAccepted) {
            _state.update { it.copy(showSosDisclaimer = true) }
            return
        }
        playSos()
    }

    fun onSosConfirm() {
        _state.update { it.copy(showSosDisclaimer = false, sosDisclaimerAccepted = true) }
        viewModelScope.launch { settingsRepository.acceptSosDisclaimer() }
        playSos()
    }

    fun onSosDismiss() {
        _state.update { it.copy(showSosDisclaimer = false) }
    }

    fun onToggleConstantLight() {
        if (_state.value.isSending) return
        val next = !_state.value.constantLightOn
        transmitter.setConstantLight(next, _state.value.resolvedChannel)
        _state.update { it.copy(constantLightOn = transmitter.isConstantLightOn()) }
    }

    fun onPauseToggle() {
        val current = _state.value.playback
        when (current) {
            is PlaybackState.Paused -> transmitter.resume()
            is PlaybackState.Transmitting -> transmitter.pause()
            else -> Unit
        }
    }

    fun onStop() = transmitter.stop()

    override fun onCleared() {
        persistJob?.cancel()
        super.onCleared()
    }

    // ------------------------------------------------------------------ internals

    private fun sendMessage(loop: Boolean) {
        val current = _state.value
        if (current.script.isEmpty) {
            _messages.trySend("先输入要发送的报文")
            return
        }
        val thermal = thermalGuard.refresh()
        if (thermal.requiresThrottle) {
            _messages.trySend("设备温度偏高（${thermal.displayName}），建议降低速度或间歇发送")
        }
        val channel = transmitter.resolveChannel(current.channelPreference, current.timing)
        transmitter.play(current.script, loop = loop, channel = channel)
        viewModelScope.launch {
            historyRepository.record(current.message, current.wpm, channel)
            settingsRepository.setLastMessage(current.message)
        }
    }

    private fun playSos() {
        val current = _state.value
        // 用三个字母 S O S 编码（27t = 3240 ms @ 10 WPM），与规划文档 §11.1 的校验值一致；
        // prosign 的连续形态会省掉字符间隔而变成 23t，人工识读时反而更易误判。
        val script = ScriptCompiler.compile(
            encoder.encode("SOS"),
            current.timing,
        )
        val channel = transmitter.resolveChannel(current.channelPreference, current.timing)
        if (current.resolvedChannel != channel) {
            _state.update { it.copy(resolvedChannel = channel) }
        }
        transmitter.play(script, loop = true, channel = channel)
        viewModelScope.launch { historyRepository.record("SOS", current.wpm, channel) }
    }

    private fun applyMessage(text: String, persist: Boolean) {
        _state.update { it.copy(message = text) }
        recompile()
        if (persist) {
            persistJob?.cancel()
            persistJob = viewModelScope.launch {
                delay(PERSIST_DEBOUNCE_MILLIS)
                settingsRepository.setLastMessage(text)
            }
        }
    }

    /** 报文或速度变化后重新编译绝对时间轴。纯 CPU 操作，10 µs 量级，无需后台线程。 */
    private fun recompile() {
        val current = _state.value
        val normalized = TextNormalizer.normalize(current.message)
        val code = encoder.encodeNormalized(normalized)
        val script = ScriptCompiler.compile(code, current.timing)
        val unsupported = current.message.count { char ->
            char != ' ' && char != '?' && TextNormalizer.mapChar(char) == '?'
        }
        val resolved = transmitter.resolveChannel(current.channelPreference, current.timing)
        _state.update {
            it.copy(
                code = code,
                script = script,
                unsupportedCount = unsupported,
                resolvedChannel = resolved,
            )
        }
    }

    private fun abortMessage(reason: AbortReason): String = when (reason) {
        AbortReason.ThermalProtection -> "已因过热保护停止发送"
        AbortReason.ExternalTakeover -> "手电筒已被其他应用占用，发送已停止"
        AbortReason.TorchUnavailable -> "当前通道不可用，请切换到屏幕闪光"
        AbortReason.UserStopped -> "已停止发送"
        AbortReason.Error -> "发送中断"
    }

    companion object {
        const val MAX_LENGTH = 200
        private const val PERSIST_DEBOUNCE_MILLIS = 400L
        private const val COMPLETED_DISPLAY_MILLIS = 1400L

        fun factory(
            settingsRepository: SettingsRepository,
            historyRepository: HistoryRepository,
            transmitter: MorseTransmitter,
            thermalGuard: ThermalGuard,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SenderViewModel(settingsRepository, historyRepository, transmitter, thermalGuard)
            }
        }
    }
}
