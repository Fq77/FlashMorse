package com.flashmorse.feature.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.flashmorse.core.designsystem.theme.FlashMorseTheme
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.PlaybackState
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.morse.codec.MorseEncoder
import com.flashmorse.core.morse.timing.ScriptCompiler
import top.yukonga.miuix.kmp.basic.Scaffold

/**
 * 发报页预览。
 *
 * 这条预览能成立，正是因为 `SenderScreen` 只接收 State + 回调，
 * 不直接引用 ViewModel（架构文档 §5.6）。
 */
@Preview(name = "发报 · 空闲", showBackground = true)
@Composable
private fun SenderScreenIdlePreview() {
    val timing = TimingProfile.forWpm(10)
    val code = MorseEncoder().encode("SOS")
    val script = ScriptCompiler.compile(code, timing)

    FlashMorseTheme {
        Scaffold {
            SenderScreen(
                state = SenderUiState(
                    message = "SOS",
                    wpm = 10,
                    timing = timing,
                    channelPreference = ChannelPreference.Auto,
                    resolvedChannel = TransmissionChannel.Led,
                    playback = PlaybackState.Idle,
                    script = script,
                    code = code,
                ),
                onMessageChange = {},
                onPrimaryAction = {},
                onSosClick = {},
                onOpenManual = {},
                onToggleLight = {},
                onTorchBrightnessPreview = {},
                onTorchBrightnessCommit = {},
                onStop = {},
            )
        }
    }
}

@Preview(name = "发报 · 发送中", showBackground = true)
@Composable
private fun SenderScreenTransmittingPreview() {
    val timing = TimingProfile.forWpm(10)
    val code = MorseEncoder().encode("HELLO")
    val script = ScriptCompiler.compile(code, timing)

    FlashMorseTheme {
        Scaffold {
            SenderScreen(
                state = SenderUiState(
                    message = "HELLO",
                    wpm = 10,
                    timing = timing,
                    resolvedChannel = TransmissionChannel.Led,
                    playback = PlaybackState.Transmitting(
                        script = script,
                        currentIndex = 4,
                        currentSymbol = com.flashmorse.core.model.MorseSymbol.Dot,
                        elapsedMs = 900,
                        estimatedRemainingMs = 2400,
                    ),
                    script = script,
                    code = code,
                ),
                onMessageChange = {},
                onPrimaryAction = {},
                onSosClick = {},
                onOpenManual = {},
                onToggleLight = {},
                onTorchBrightnessPreview = {},
                onTorchBrightnessCommit = {},
                onStop = {},
            )
        }
    }
}
