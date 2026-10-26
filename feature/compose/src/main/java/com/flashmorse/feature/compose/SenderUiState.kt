package com.flashmorse.feature.compose

import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.MorseCode
import com.flashmorse.core.model.PlaybackState
import com.flashmorse.core.model.ScriptCell
import com.flashmorse.core.model.SendScript
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.model.isActive
import com.flashmorse.core.torch.ThermalLevel

/** 发报页的不可变 UI 状态。 */
data class SenderUiState(
    val message: String = "SOS",
    val wpm: Int = TimingProfile.DEFAULT_WPM,
    val timing: TimingProfile = TimingProfile.forWpm(TimingProfile.DEFAULT_WPM),
    val channelPreference: ChannelPreference = ChannelPreference.Auto,
    val resolvedChannel: TransmissionChannel = TransmissionChannel.Led,
    val ledAvailable: Boolean = true,
    val playback: PlaybackState = PlaybackState.Idle,
    val script: SendScript = SendScript.Empty,
    val code: MorseCode = MorseCode.Empty,
    val hapticFeedback: Boolean = true,
    val loopByDefault: Boolean = false,
    val constantLightOn: Boolean = false,
    val thermalLevel: ThermalLevel = ThermalLevel.Unknown,
    val sosDisclaimerAccepted: Boolean = false,
    val showSosDisclaimer: Boolean = false,
    /** 归一化后无法编码的字符数（被替换为 `?`），用于就地提示而不是静默吞掉。 */
    val unsupportedCount: Int = 0,
) {
    val isSending: Boolean get() = playback.isActive

    /**
     * 正在播放的脚本。
     *
     * SOS 循环与短语库试发都不经过输入框的 [script]，中央字符、符号流和总时长
     * 必须跟着**真正在播的那条时间轴**走，否则发 SOS 时中间会是空的。
     */
    private val playingScript: SendScript?
        get() = (playback as? PlaybackState.Transmitting)?.script

    private val activeScript: SendScript get() = playingScript ?: script

    val cells: List<ScriptCell> get() = activeScript.cells

    /** 当前播放到的符号单元；空闲时为 -1。 */
    val activeIndex: Int
        get() = when (val p = playback) {
            is PlaybackState.Transmitting -> p.currentIndex
            is PlaybackState.Completed -> script.cells.lastIndex
            else -> -1
        }

    val progress: Float
        get() = when (val p = playback) {
            is PlaybackState.Transmitting -> {
                val total = p.script.totalNanos.coerceAtLeast(1L)
                val elapsed = (p.elapsedMs * 1_000_000L).coerceIn(0L, total)
                elapsed.toFloat() / total
            }

            is PlaybackState.Completed -> 1f
            else -> 0f
        }

    /** 当前符号对应字符，用于发送中的大字回显。 */
    val currentChar: Char?
        get() {
            val index = activeIndex
            if (index < 0) return null
            val playing = activeScript
            val letterIndex = playing.cells.getOrNull(index)?.letterIndex ?: return null
            return playing.code.letters.getOrNull(letterIndex)?.char
        }

    val currentPattern: String?
        get() {
            val index = activeIndex
            if (index < 0) return null
            val playing = activeScript
            val letterIndex = playing.cells.getOrNull(index)?.letterIndex ?: return null
            return playing.code.letters.getOrNull(letterIndex)?.pattern
        }

    val remainingMillis: Long
        get() = (playback as? PlaybackState.Transmitting)?.estimatedRemainingMs ?: 0L

    val elapsedMillis: Long
        get() = (playback as? PlaybackState.Transmitting)?.elapsedMs ?: 0L

    val isLooping: Boolean
        get() = (playback as? PlaybackState.Transmitting)?.loop == true

    /** 含无法编码字符时在界面上就地提示。 */
    val hasUnsupportedChars: Boolean get() = unsupportedCount > 0

    val isEmpty: Boolean get() = code.isEmpty

    val letterCount: Int get() = activeScript.code.letterCount

    val estimatedTotalMillis: Long get() = activeScript.totalMillis
}
