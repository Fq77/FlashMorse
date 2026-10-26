package com.flashmorse.feature.manual

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.component.MorsePattern
import com.flashmorse.core.designsystem.icon.FmIcons
import com.flashmorse.core.model.TransmissionChannel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 手动点按发报（F-03）。
 *
 * 整页只有一个操作面：按住即发光。所有反馈（点/划判定、已解码文本）都在松手后立刻可见，
 * 因为戴手套、低温、夜间是这一页的默认使用环境。
 */
@Composable
fun ManualScreen(
    state: ManualUiState,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 220.dp),
            insideMargin = PaddingValues(0.dp),
            cornerRadius = 24.dp,
            colors = if (state.pressed) {
                CardDefaults.defaultColors(
                    color = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                )
            } else {
                CardDefaults.defaultColors()
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state.unitMillis, state.hapticFeedback) {
                        detectTapGestures(
                            onPress = {
                                onPressStart()
                                if (state.hapticFeedback) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                tryAwaitRelease()
                                onPressEnd()
                            },
                        )
                    }
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = when {
                        state.pressed && state.pressedMillis >= state.dashThresholdMillis -> "划"
                        state.pressed -> "点"
                        state.pattern.isNotEmpty() -> "继续按键"
                        else -> "按住发报"
                    },
                    style = MiuixTheme.textStyles.title1,
                    color = if (state.pressed) colorScheme.onPrimary else colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.pattern.isNotEmpty()) {
                        MorsePattern(
                            pattern = state.pattern,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                            color = if (state.pressed) colorScheme.onPrimary else colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (state.pressed) {
                        "已按住 ${state.pressedMillis} ms"
                    } else {
                        "轻点 = 点 · 按住 ≥ ${state.dashThresholdMillis} ms = 划"
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (state.pressed) colorScheme.onPrimary else colorScheme.onBackgroundVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "松手 3 个单位后自动断字，7 个单位后断词 · ${state.wpm} WPM",
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (state.pressed) colorScheme.onPrimary else colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "解码结果",
                        style = MiuixTheme.textStyles.subtitle,
                        color = colorScheme.onBackground,
                    )
                    Text(
                        text = if (state.isEmpty) {
                            "等待输入"
                        } else {
                            "${state.displayText.count { it != ' ' }} 个字符"
                        },
                        style = MiuixTheme.textStyles.footnote2,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.isEmpty) "—" else state.displayText,
                    style = MiuixTheme.textStyles.title3,
                    color = if (state.isEmpty) colorScheme.onSurfaceVariantSummary else colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 34.dp),
                )
                state.lastChar?.let { char ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "最近一个字符：${if (char == '?') "无法识别" else char.toString()}",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SmallTile(
                icon = FmIcons.Undo,
                label = "退格",
                container = colorScheme.tertiaryContainer,
                content = colorScheme.onTertiaryContainer,
                onClick = onBackspace,
                modifier = Modifier.weight(1f),
            )
            SmallTile(
                icon = FmIcons.Delete,
                label = "清空",
                container = colorScheme.errorContainer,
                content = colorScheme.onErrorContainer,
                onClick = onClear,
                modifier = Modifier.weight(1f),
            )
            SmallTile(
                icon = FmIcons.Send,
                label = "发送",
                container = colorScheme.primary,
                content = colorScheme.onPrimary,
                onClick = onSend,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun SmallTile(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(58.dp),
            cornerRadius = 29.dp,
            colors = ButtonDefaults.buttonColors(color = container, contentColor = content),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** 通道提示：屏幕通道时给出说明。 */
@Composable
fun ManualChannelHint(channel: TransmissionChannel, modifier: Modifier = Modifier) {
    if (channel != TransmissionChannel.Screen) return
    Text(
        text = "未检测到闪光灯，已使用屏幕闪光通道",
        style = MiuixTheme.textStyles.footnote2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier,
    )
}
