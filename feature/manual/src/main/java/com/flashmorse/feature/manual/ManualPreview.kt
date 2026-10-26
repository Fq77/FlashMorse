package com.flashmorse.feature.manual

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.flashmorse.core.designsystem.theme.FlashMorseTheme
import com.flashmorse.core.model.TransmissionChannel
import top.yukonga.miuix.kmp.basic.Scaffold

/** 手动发报页预览（不依赖 ViewModel）。 */
@Preview(name = "手动发报", showBackground = true)
@Composable
private fun ManualScreenPreview() {
    FlashMorseTheme {
        Scaffold {
            ManualScreen(
                state = ManualUiState(
                    wpm = 10,
                    unitMillis = 120,
                    channel = TransmissionChannel.Led,
                    pattern = ".-",
                    text = "HE",
                    lastChar = 'E',
                ),
                onPressStart = {},
                onPressEnd = {},
                onBackspace = {},
                onClear = {},
                onSend = {},
            )
        }
    }
}
