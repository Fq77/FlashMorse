package com.flashmorse.feature.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.morse.codec.MorseDecoder
import com.flashmorse.core.morse.codec.MorseEncoder
import com.flashmorse.core.morse.timing.HighResolutionClock
import com.flashmorse.core.morse.timing.ScriptCompiler
import com.flashmorse.core.morse.timing.SystemHighResolutionClock
import com.flashmorse.core.torch.MorseTransmitter
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 手动点按发报（F-03）。
 *
 * 交互契约：按住 = 划、轻点 = 点；松手后按标准间隔自动断字断词——
 * 3t 无输入即断字，累计 7t 即断词。
 */
class ManualViewModel(
    private val settingsRepository: SettingsRepository,
    private val transmitter: MorseTransmitter,
    private val clock: HighResolutionClock = SystemHighResolutionClock,
) : ViewModel() {

    private val encoder = MorseEncoder()

    private val _state = MutableStateFlow(ManualUiState())
    val state: StateFlow<ManualUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var pressStartNanos = 0L
    private var gapJob: Job? = null
    private var tickJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        wpm = settings.wpm,
                        unitMillis = settings.timingProfile.unitMillis,
                        hapticFeedback = settings.hapticFeedback,
                        channel = if (transmitter.ledAvailable) {
                            TransmissionChannel.Led
                        } else {
                            TransmissionChannel.Screen
                        },
                    )
                }
            }
        }
    }

    fun onPressStart() {
        if (_state.value.pressed) return
        gapJob?.cancel()
        if (!transmitter.manualKey(down = true, channel = _state.value.channel)) {
            _messages.trySend("当前通道不可用")
            return
        }
        pressStartNanos = clock.nanos()
        _state.update { it.copy(pressed = true, pressedMillis = 0L) }
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive && _state.value.pressed) {
                val elapsed = (clock.nanos() - pressStartNanos) / NANOS_PER_MILLI
                _state.update { it.copy(pressedMillis = elapsed) }
                delay(TICK_MILLIS)
            }
        }
    }

    fun onPressEnd() {
        if (!_state.value.pressed) return
        val elapsedMillis = (clock.nanos() - pressStartNanos) / NANOS_PER_MILLI
        transmitter.manualKey(down = false, channel = _state.value.channel)
        tickJob?.cancel()

        val symbol = if (elapsedMillis >= _state.value.dashThresholdMillis) '-' else '.'
        _state.update {
            it.copy(
                pressed = false,
                pressedMillis = elapsedMillis,
                pattern = (it.pattern + symbol).take(MAX_PATTERN_LENGTH),
            )
        }
        scheduleGapTracking()
    }

    fun onBackspace() {
        gapJob?.cancel()
        val current = _state.value
        when {
            current.pattern.isNotEmpty() -> _state.update { it.copy(pattern = it.pattern.dropLast(1)) }
            current.text.isNotEmpty() -> _state.update {
                it.copy(text = it.text.dropLast(1), lastChar = null)
            }
        }
    }

    fun onClear() {
        gapJob?.cancel()
        _state.update { it.copy(pattern = "", text = "", lastChar = null) }
    }

    /** 把当前文本编译成绝对时间轴并发送。 */
    fun onSend() {
        gapJob?.cancel()
        commitCurrentChar()
        val current = _state.value
        val text = current.displayText
        if (text.isBlank()) {
            _messages.trySend("还没有可发送的内容")
            return
        }
        val timing = TimingProfile.forWpm(current.wpm)
        val script = ScriptCompiler.compile(encoder.encode(text), timing)
        val channel = transmitter.resolveChannel(ChannelPreference.Auto, timing)
        transmitter.play(script, loop = false, channel = channel)
    }

    override fun onCleared() {
        tickJob?.cancel()
        gapJob?.cancel()
        transmitter.manualKey(down = false, channel = _state.value.channel)
        super.onCleared()
    }

    // ------------------------------------------------------------------ internals

    private fun scheduleGapTracking() {
        gapJob?.cancel()
        gapJob = viewModelScope.launch {
            delay(_state.value.unitMillis.toLong() * CHAR_GAP_UNITS)
            commitCurrentChar()
            delay(_state.value.unitMillis.toLong() * (WORD_GAP_UNITS - CHAR_GAP_UNITS))
            commitWordBreak()
        }
    }

    private fun commitCurrentChar() {
        val pattern = _state.value.pattern
        if (pattern.isEmpty()) return
        val char = MorseDecoder.charFor(pattern)
        _state.update {
            it.copy(
                text = it.text + char,
                pattern = "",
                lastChar = char,
            )
        }
    }

    private fun commitWordBreak() {
        _state.update {
            if (it.text.isEmpty() || it.text.endsWith(" ")) it else it.copy(text = it.text + " ")
        }
    }

    companion object {
        const val MAX_PATTERN_LENGTH = 7
        private const val CHAR_GAP_UNITS = 3
        private const val WORD_GAP_UNITS = 7
        private const val TICK_MILLIS = 32L
        private const val NANOS_PER_MILLI = 1_000_000L

        fun factory(
            settingsRepository: SettingsRepository,
            transmitter: MorseTransmitter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { ManualViewModel(settingsRepository, transmitter) }
        }
    }
}
