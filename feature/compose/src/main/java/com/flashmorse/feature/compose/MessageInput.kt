package com.flashmorse.feature.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.component.MorseStream
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 报文输入 + 符号流预览。
 *
 * 输入框在发送期间保持占位（禁用态），不隐藏：整块消失会让下方符号流上跳 50 dp 以上，
 * 与「不挤压 / 不隐藏 / 布局稳定」的设计前提自相矛盾。
 */
@Composable
internal fun MessageInput(
    state: SenderUiState,
    onMessageChange: (String) -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = state.message,
            onValueChange = onMessageChange,
            label = if (state.isSending) "发送中…" else "报文",
            useLabelAsPlaceholder = true,
            singleLine = true,
            enabled = !state.isSending,
            // 输入态下发送卡让位给键盘，因此键盘必须能一键收起回到发报
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    contentAlignment = if (state.cells.isEmpty()) {
                        Alignment.Center
                    } else {
                        Alignment.CenterStart
                    },
                ) {
                    if (state.cells.isEmpty()) {
                        Text(
                            text = "请在上方输入报文，支持 A-Z、0-9 或常用标点。",
                            style = MiuixTheme.textStyles.footnote1,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    } else {
                        MorseStream(
                            cells = state.cells,
                            activeIndex = state.activeIndex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp),
                        )
                    }
                }

                if (state.hasUnsupportedChars) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "含 ${state.unsupportedCount} 个无法编码的字符，将以 ? 发送",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colorScheme.error,
                    )
                }
            }
        }
    }
}
