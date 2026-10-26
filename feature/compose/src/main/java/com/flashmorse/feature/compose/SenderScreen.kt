package com.flashmorse.feature.compose

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import com.flashmorse.core.designsystem.component.ThiingsSendIcon
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.component.MorseStream
import com.flashmorse.core.designsystem.icon.FmIcons
import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.PlaybackState
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.torch.ThermalLevel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import kotlin.math.roundToInt
import java.util.Locale

/**
 * 发报页：页面的唯一视觉焦点是中间的大发送卡（规划文档 §7.5）。
 *
 * 自上而下：报文与符号流 → 大发送卡 → 一键操作。速度只在设置页调节，此处不重复。
 */
@Composable
fun SenderScreen(
    state: SenderUiState,
    onMessageChange: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onSosClick: () -> Unit,
    onSosConfirm: () -> Unit,
    onSosDismiss: () -> Unit,
    onOpenManual: () -> Unit,
    onToggleLight: () -> Unit,
    onTorchBrightnessPreview: (Int) -> Unit,
    onTorchBrightnessCommit: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 常亮即"手电筒模式"：整页切换成灯面 + 调光，不再显示报文与发送卡，
    // 否则用户分不清当前处于哪种状态。
    if (state.constantLightOn) {
        TorchScreen(
            state = state,
            onPreview = onTorchBrightnessPreview,
            onCommit = onTorchBrightnessCommit,
            onTurnOff = onToggleLight,
        )
        return
    }

    val scrollState = rememberScrollState()

    // 整页可滚动 + 组件使用固定高度：
    // 输入法弹出时窗口变矮，视口随之变小，但任何组件都不会被压缩、隐藏或跳位，
    // 用户只是需要向上滚动才能看到下方的发送区——这是安卓里唯一能同时满足
    //「不挤压 / 不隐藏 / 布局稳定」三者的做法（其余方案必然改变某处尺寸）。
    Column(
        modifier = modifier
            .fillMaxSize()
            // edge-to-edge 下 adjustResize 不再自动缩小窗口，必须显式消费 IME inset：
            // 只收缩「滚动视口」，组件自身的尺寸与相对位置保持不变，因此没有挤压或跳位，
            // 被键盘挡住的发送区可以通过滚动到达。
            .imePadding()
            .padding(horizontal = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MessageInput(state = state, onMessageChange = onMessageChange)

        SendCard(
            state = state,
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(SEND_CARD_HEIGHT),
        )

        ActionRow(
            state = state,
            onSosClick = onSosClick,
            onOpenManual = onOpenManual,
            onToggleLight = onToggleLight,
            onStop = onStop,
        )

        Spacer(modifier = Modifier.height(4.dp))
    }

    SenderSosDisclaimerDialog(
        state = state,
        onConfirm = onSosConfirm,
        onDismiss = onSosDismiss,
    )
}

/** 发送卡固定高度：不随窗口/键盘变化，是「布局稳定」的关键。 */
private val SEND_CARD_HEIGHT = 340.dp

// ------------------------------------------------------------------ 报文 + 符号流

@Composable
private fun MessageInput(
    state: SenderUiState,
    onMessageChange: (String) -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // 发报过程中输入框隐藏（此时它只是干扰），摩斯流保留并自动跟随播放位置
        if (!state.isSending) {
            TextField(
                value = state.message,
                onValueChange = onMessageChange,
            label = "报文",
            useLabelAsPlaceholder = true,
            singleLine = true,
            enabled = !state.isSending,
            // 输入态下发送卡让位给键盘，因此键盘必须能一键收起回到发报
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
            modifier = Modifier.fillMaxWidth(),
            )
        }

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
                            text = "请在上方输入报文，支持A-Z、0-9或常用标点。",
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

// -------------------------------------------------------------------- 大发送卡

@Composable
private fun SendCard(
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
            // 键盘弹出时高度会被压缩，此时退化为紧凑行；否则用环形焦点布局
            val availableHeight = this.maxHeight
            val compact = availableHeight < 208.dp
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

/** 环形焦点：空闲为描边空环，发送时为进度环，中央显示当前符号或状态图标。 */
@Composable
private fun SendRing(
    state: SenderUiState,
    sending: Boolean,
    completed: Boolean,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    charStyle: androidx.compose.ui.text.TextStyle,
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

// -------------------------------------------------------------------- 一键操作

@Composable
private fun ActionRow(
    state: SenderUiState,
    onSosClick: () -> Unit,
    onOpenManual: () -> Unit,
    onToggleLight: () -> Unit,
    onStop: () -> Unit,
) {
    if (state.isSending) {
        // 暂停已移除：对光信号来说"暂停几秒"对接收方等于乱码，只保留明确的停止。
        // 实色圆按钮 + 红色：与"删除/中止"同一套色彩语义。
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = onStop,
                modifier = Modifier.size(60.dp),
                cornerRadius = 30.dp,
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.error,
                    contentColor = MiuixTheme.colorScheme.onError,
                ),
            ) {
                Icon(
                    imageVector = FmIcons.Close,
                    contentDescription = "停止发送",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // SOS 独占一行：这是本应用的道德与安全底线入口，必须比「手动 / 手电筒」更重
        // SOS 与「手动发报」保持同一尺寸、同一字号：只有颜色不同（红 = 紧急）
        Button(
            onClick = onSosClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.error,
                contentColor = MiuixTheme.colorScheme.onError,
            ),
        ) {
            Text(
                text = "SOS",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onError,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryAction(
                label = "手动发报",
                onClick = onOpenManual,
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(
                label = "手电筒常亮",
                onClick = onToggleLight,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 主操作实色按钮：Miuix 主题蓝，两个次级入口共用同一形态。 */
@Composable
private fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        colors = ButtonDefaults.buttonColorsPrimary(),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onPrimary,
        )
    }
}


/** 次要操作块与 SOS 行共用的高度基准。 */
private val ACTION_TILE_HEIGHT = 58.dp

// -------------------------------------------------------------------- 免责声明

@Composable
private fun SenderSosDisclaimerDialog(
    state: SenderUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        title = "确认发送求救信号",
        summary = "请确认当前处于真实的紧急情况。滥用求救信号可能承担法律责任。\n\n" +
            "SOS 是辅助手段，不能替代专业救援设备与 112 / 110 报警。",
        show = state.showSosDisclaimer,
        onDismissRequest = onDismiss,
    ) {
        TextButton(
            text = "我确认，开始发送",
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            text = "取消",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// -------------------------------------------------------------------- 手电筒

/**
 * 手电筒整页界面：完全用 Canvas 绘制的场景——墙面光斑、光束、筒身、
 * 筒身进度槽、底部开关，全部元素的**位置固定**，只有亮度随 [fraction] 连续变化。
 *
 * 交互：在场景任意位置上下拖动无级调光；拖动过程走 [onPreview]（即时驱动 LED
 * 与界面），松手才走 [onCommit] 落盘——反过来（每次位移都写 DataStore）
 * 会把磁盘写放大到每秒几十次。
 */
@Composable
private fun TorchScreen(
    state: SenderUiState,
    onPreview: (Int) -> Unit,
    onCommit: (Int) -> Unit,
    onTurnOff: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    // 已提交值单独持有：拖动结束到 DataStore 往返之间有一个空窗，
    // 若直接回落到 state.brightnessPercent，读数会先跳回旧值再跳到新值。
    var committedPercent by remember { mutableIntStateOf(state.brightnessPercent) }
    LaunchedEffect(state.brightnessPercent) { committedPercent = state.brightnessPercent }
    val fraction = dragFraction ?: Brightness.percentToFraction(committedPercent)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0806))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        dragFraction?.let { f ->
                            val p = (f * 100f).roundToInt()
                            onCommit(p)
                            committedPercent = p
                        }
                        dragFraction = null
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val base = dragFraction ?: Brightness.percentToFraction(committedPercent)
                        // 拖 60% 场景高度即覆盖 0–100%：阻尼偏小，调节更跟手
                        val next = (base - dragAmount / size.height * 1.6f).coerceIn(0f, 1f)
                        dragFraction = next
                        onPreview((next * 100f).roundToInt())
                    },
                )
            },
    ) {
        TorchScene(fraction = fraction, modifier = Modifier.fillMaxSize())

        // 底部关闭钮：Miuix 红实色 + Close 图标（与全局"停止/删除"同一色彩语义）
        Button(
            onClick = onTurnOff,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 20.dp)
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(
                color = colorScheme.error,
                contentColor = colorScheme.onError,
            ),
        ) {
            Icon(
                imageVector = FmIcons.Close,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "关闭手电筒",
                style = MiuixTheme.textStyles.body1,
                color = colorScheme.onError,
            )
        }
    }
}

/**
 * 手电筒场景：一层 Canvas 从后往前画「墙面 → 光斑 → 光束 → 筒身 → 进度槽」。
 * 所有几何只依赖画布尺寸，所有颜色/透明度/半径都由 [fraction] 连续插值，
 * 因此亮度变化全程平滑、无任何元素位移。
 */
@Composable
private fun TorchScene(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val f = fraction.coerceIn(0f, 1f)
        val headY = h * 0.40f          // 灯头顶沿
        val bodyBottom = h * 0.84f     // 筒身更修长
        val bodyTopHalf = w * 0.34f    // 前（灯头）宽
        val bodyBotHalf = w * 0.29f    // 后（尾）窄：手电筒前宽后窄，差值收敛

        // 1. 墙面：暖色底亮度随 f 上升（熄灭时近黑）
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    lerp(Color(0xFF070605), Color(0xFF33281A), f),
                    lerp(Color(0xFF050404), Color(0xFF241C11), f),
                ),
            ),
        )

        // 2. 墙面光斑：中心放在灯头**上方**，光晕只笼罩头部以上的墙面——
        //    下缘随径向衰减自然变淡、到筒身处已接近透明，不出现硬切线
        val glowR = w * (0.26f + 0.46f * f)
        val glowCenter = Offset(w / 2f, headY - glowR * 0.6f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFE7B0).copy(alpha = 0.85f * f),
                    Color(0xFFFFE7B0).copy(alpha = 0f),
                ),
                center = glowCenter,
                radius = glowR,
            ),
            radius = glowR,
            center = glowCenter,
        )
        // 贴头小光晕：让灯头正上方的墙面最亮，与灯头顶沿高光衔接
        val nearR = glowR * 0.55f
        val nearCenter = Offset(w / 2f, headY - nearR * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF3D2).copy(alpha = 0.70f * f),
                    Color(0xFFFFF3D2).copy(alpha = 0f),
                ),
                center = nearCenter,
                radius = nearR,
            ),
            radius = nearR,
            center = nearCenter,
        )

        // 3. 光柱：从灯头向上展开的锥形，随亮度变宽变亮（f=0 时几乎不可见）
        val beamSpread = w * (0.24f + 0.40f * f)
        val beamPath = Path().apply {
            moveTo(w / 2f - bodyTopHalf * 0.9f, headY)
            lineTo(w / 2f + bodyTopHalf * 0.9f, headY)
            lineTo(w / 2f + beamSpread, -h * 0.02f)
            lineTo(w / 2f - beamSpread, -h * 0.02f)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFFFFF3D2).copy(alpha = 0.72f * f),
                    Color(0xFFFFF3D2).copy(alpha = 0.04f * f),
                ),
            ),
        )

        // 4. 灯头顶沿高光线：亮度随 f 全程连续（熄灭时不可见，无恒亮基线）
        drawLine(
            color = Color(0xFFFFF6DC).copy(alpha = 0.92f * f),
            start = Offset(w / 2f - bodyTopHalf * 0.92f, headY),
            end = Offset(w / 2f + bodyTopHalf * 0.92f, headY),
            strokeWidth = 2.dp.toPx(),
        )

        // 5. 筒身（梯形）：底色随亮度轻微提升，再叠两侧暗角把主体"立"出来
        val bodyPath = Path().apply {
            moveTo(w / 2f - bodyTopHalf, headY)
            lineTo(w / 2f + bodyTopHalf, headY)
            lineTo(w / 2f + bodyBotHalf, bodyBottom)
            lineTo(w / 2f - bodyBotHalf, bodyBottom)
            close()
        }
        drawPath(bodyPath, lerp(Color(0xFF191410), Color(0xFF554324), 0.25f + 0.75f * f))
        drawPath(
            path = bodyPath,
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Black.copy(alpha = 0.38f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.38f),
                ),
            ),
        )

        // 6. 筒身进度槽（固定）+ 从底部向上生长的填充条
        val slotW = 5.dp.toPx()
        val slotTop = headY + (bodyBottom - headY) * 0.18f
        val slotBottom = bodyBottom - (bodyBottom - headY) * 0.18f
        val slotH = slotBottom - slotTop
        drawRoundRect(
            color = Color(0xFF0F0C08).copy(alpha = 0.92f),
            topLeft = Offset(w / 2f - slotW / 2f, slotTop),
            size = Size(slotW, slotH),
            cornerRadius = CornerRadius(slotW / 2f, slotW / 2f),
        )
        val fillH = slotH * f
        if (fillH > 1f) {
            drawRoundRect(
                color = Color(0xFFFFF6DC).copy(alpha = 0.45f + 0.55f * f),
                topLeft = Offset(w / 2f - slotW / 2f, slotBottom - fillH),
                size = Size(slotW, fillH),
                cornerRadius = CornerRadius(slotW / 2f, slotW / 2f),
            )
        }
    }
}



// ---------------------------------------------------------------------- helpers

private fun charLabel(state: SenderUiState): String {
    val char = state.currentChar ?: return ""
    return if (char == '§') "SOS" else char.toString()
}

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

internal fun formatSeconds(millis: Long): String {
    if (millis <= 0L) return "0 秒"
    val seconds = millis / 1000.0
    return if (seconds < 10.0) {
        String.format(Locale.US, "%.1f 秒", seconds)
    } else {
        "${(seconds + 0.5).toLong()} 秒"
    }
}
