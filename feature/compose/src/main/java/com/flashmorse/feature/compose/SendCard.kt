package com.flashmorse.feature.compose

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.component.ThiingsSendIcon
import com.flashmorse.core.designsystem.icon.FmIcons
import com.flashmorse.core.model.PlaybackState
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.torch.ThermalLevel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.util.Locale

/** 发送卡固定高度：不随窗口/键盘变化，是「布局稳定」的关键。 */
internal val SEND_CARD_HEIGHT = 340.dp

/** 横屏高度：落在 [SendCard] 的 compact 阈值（208 dp）之内。 */
internal val SEND_CARD_LANDSCAPE_HEIGHT = 200.dp

/** compact 与环形布局的分界高度。 */
private val COMPACT_THRESHOLD = 208.dp

/**
 * 页面的唯一视觉焦点：一张整体可点按的卡片。
 *
 * 两种形态共存于同一张卡里——视口够高时用环形焦点（进度环 + 中央符号），
 * 键盘弹出或横屏时退化为紧凑行（小环 + 左右排布）。形态切换由实际高度决定，
 * 而不是由「键盘是否弹出」这类外部状态决定，因此不会出现两者打架的中间态。
 */
@Composable
internal fun SendCard(
    state: SenderUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val sending = state.isSending
    val completed = state.playback is PlaybackState.Completed

    Card(
        modifier = modifier.heightIn(min = 96.dp),
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 26.dp,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            val availableHeight = this.maxHeight
            val compact = availableHeight < COMPACT_THRESHOLD
            val ringSize = (availableHeight - 118.dp).coerceIn(88.dp, 148.dp)

            if (compact) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val compactRing = if (availableHeight < 60.dp) 38.dp else 48.dp
                    SendRing(
                        state = state,
                        sending = sending,
                        completed = completed,
                        size = compactRing,
                        iconSize = compactRing * 0.46f,
                        charStyle = MiuixTheme.textStyles.title4,
                    )
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = sendTitle(state),
                            style = MiuixTheme.textStyles.subtitle,
                            color = colorScheme.onBackground,
                        )
                        if (availableHeight >= 56.dp) {
                            Text(
                                text = sendSubtitle(state),
                                style = MiuixTheme.textStyles.footnote2,
                                color = if (sending) colorScheme.primary else colorScheme.onBackgroundVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SendRing(
                        state = state,
                        sending = sending,
                        completed = completed,
                        size = ringSize,
                        iconSize = 60.dp,
                        charStyle = MiuixTheme.textStyles.title1,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = sendTitle(state),
                        style = MiuixTheme.textStyles.title3,
                        color = if (state.isEmpty && !sending && !completed) {
                            colorScheme.onBackgroundVariant
                        } else {
                            colorScheme.onBackground
                        },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = sendSubtitle(state),
                        style = MiuixTheme.textStyles.footnote1,
                        color = if (sending) colorScheme.primary else colorScheme.onBackgroundVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (!sending && !completed && !state.isEmpty) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = channelLabel(state),
                            style = MiuixTheme.textStyles.footnote2,
                            color = colorScheme.onSurfaceVariantSummary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** 环形焦点：空闲为 3D 图标，发送时为进度环，完成后为对勾。 */
@Composable
private fun SendRing(
    state: SenderUiState,
    sending: Boolean,
    completed: Boolean,
    size: Dp,
    iconSize: Dp,
    charStyle: TextStyle,
) {
    val colorScheme = MiuixTheme.colorScheme

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (sending) {
            CircularProgressIndicator(
                progress = rememberSmoothProgress(state),
                size = size,
                strokeWidth = 8.dp,
            )
        }

        when {
            sending -> Text(
                text = charLabel(state),
                style = charStyle,
                color = colorScheme.onBackground,
            )

            completed -> Icon(
                imageVector = FmIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = colorScheme.primary,
            )

            // 空闲态直接展示 3D 图标（thiings.co），不再套圆形背景与描边
            else -> ThiingsSendIcon(modifier = Modifier.size(iconSize * 1.6f))
        }
    }
}

/**
 * 发送进度的逐帧外推。
 *
 * 播放器只在「灯态翻转」时发布一次状态，也就是说进度采样的间隔等于一个符号单元：
 * 5 WPM 时长达 240 ms，直接用 [SenderUiState.progress] 画环会一格一格地跳。
 * 这里以事件携带的 `elapsedMs` 为锚点，用单调时钟按帧外推到当前单元的结束时刻——
 * 单元内的时间流逝是线性的，所以外推结果与真实播放位置一致，且不会越过下一事件。
 */
@Composable
private fun rememberSmoothProgress(state: SenderUiState): Float {
    val transmitting = state.playback as? PlaybackState.Transmitting ?: return state.progress

    var progress by remember(transmitting.script, transmitting.currentIndex) {
        mutableFloatStateOf(state.progress)
    }

    LaunchedEffect(transmitting) {
        val script = transmitting.script
        val total = script.totalNanos.coerceAtLeast(1L)
        val cell = script.cells.getOrNull(transmitting.currentIndex)
        val lower = (cell?.startNanos ?: 0L).coerceIn(0L, total)
        val upper = (cell?.endNanos ?: total).coerceIn(lower, total)

        // 采样时刻来自播放线程的时钟，而不是「本帧发现新状态」的时刻，
        // 这样主线程合成稍慢也不会把进度整体推后。
        val sampledElapsed = transmitting.elapsedMs * 1_000_000L
        val sampledAt = transmitting.emittedAtNanos.takeIf { it > 0L }
            ?: SystemClock.elapsedRealtimeNanos()

        while (true) {
            withFrameNanos { }
            val elapsed = sampledElapsed + (SystemClock.elapsedRealtimeNanos() - sampledAt)
            progress = (elapsed.coerceIn(lower, upper).toFloat() / total).coerceIn(0f, 1f)
        }
    }

    return progress
}

private fun sendTitle(state: SenderUiState): String = when {
    state.playback is PlaybackState.Completed -> "已发送"
    state.playback is PlaybackState.Paused -> "已暂停"
    state.isSending && state.isLooping -> "循环发报中"
    state.isSending -> "发送中"
    state.isEmpty -> "等待报文"
    else -> "点按发报"
}

private fun sendSubtitle(state: SenderUiState): String = when {
    state.playback is PlaybackState.Completed ->
        "共 ${state.letterCount} 个字符 · ${formatSeconds(state.estimatedTotalMillis)}"

    state.isSending -> "剩余 ${formatSeconds(state.remainingMillis)} · 点按停止"
    state.isEmpty -> "在上方输入要发送的内容"
    else -> "${state.wpm} WPM · ${state.letterCount} 个字符 · 约 ${formatSeconds(state.estimatedTotalMillis)}"
}

private fun charLabel(state: SenderUiState): String = state.currentChar?.toString() ?: ""

private fun channelLabel(state: SenderUiState): String {
    if (state.isEmpty) return ""
    val base = when (state.resolvedChannel) {
        TransmissionChannel.Led -> "手电筒通道"
        TransmissionChannel.Screen -> "屏幕闪光通道"
    }
    val thermal = when (state.thermalLevel) {
        ThermalLevel.None, ThermalLevel.Light, ThermalLevel.Unknown -> ""
        else -> " · 设备${state.thermalLevel.displayName}"
    }
    return base + thermal
}

private fun formatSeconds(millis: Long): String {
    if (millis <= 0L) return "0 秒"
    val seconds = millis / 1000.0
    return if (seconds < 10.0) {
        String.format(Locale.US, "%.1f 秒", seconds)
    } else {
        "${(seconds + 0.5).toLong()} 秒"
    }
}
