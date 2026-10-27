package com.flashmorse.feature.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.icon.FmIcons
import com.flashmorse.core.model.Brightness
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 手电筒场景的全部色值。
 *
 * 这些颜色不来自 Miuix 主题：它们模拟的是「一间暗室里被手电照亮的墙面」，
 * 属于光学写实，不能随主题变化。集中在此是为了调整时只改一处，
 * 而不是在十几处 draw 调用里逐个数十六进制。
 */
private object TorchPalette {
    val ScreenBackground = Color(0xFF0A0806)
    val WallDark = Color(0xFF070605)
    val WallDarker = Color(0xFF050404)
    val WallLit = Color(0xFF33281A)
    val WallLitDeeper = Color(0xFF241C11)
    val GlowWarm = Color(0xFFFFE7B0)
    val GlowNear = Color(0xFFFFF3D2)
    val Highlight = Color(0xFFFFF6DC)
    val BodyDark = Color(0xFF191410)
    val BodyLit = Color(0xFF554324)
    val SlotBase = Color(0xFF0F0C08)
}

/**
 * 手电筒整页界面：完全用 Canvas 绘制的场景——墙面光斑、光束、筒身、
 * 筒身进度槽、底部开关，全部元素的**位置固定**，只有亮度随 [fraction] 连续变化。
 *
 * 交互：在场景任意位置上下拖动无级调光；拖动过程走 [onPreview]（即时驱动 LED
 * 与界面），松手才走 [onCommit] 落盘——反过来（每次位移都写 DataStore）
 * 会把磁盘写放大到每秒几十次。
 */
@Composable
internal fun TorchScreen(
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
            .background(TorchPalette.ScreenBackground)
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
                    lerp(TorchPalette.WallDark, TorchPalette.WallLit, f),
                    lerp(TorchPalette.WallDarker, TorchPalette.WallLitDeeper, f),
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
                    TorchPalette.GlowWarm.copy(alpha = 0.85f * f),
                    TorchPalette.GlowWarm.copy(alpha = 0f),
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
                    TorchPalette.GlowNear.copy(alpha = 0.70f * f),
                    TorchPalette.GlowNear.copy(alpha = 0f),
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
                    TorchPalette.GlowNear.copy(alpha = 0.72f * f),
                    TorchPalette.GlowNear.copy(alpha = 0.04f * f),
                ),
            ),
        )

        // 4. 灯头顶沿高光线：亮度随 f 全程连续（熄灭时不可见，无恒亮基线）
        drawLine(
            color = TorchPalette.Highlight.copy(alpha = 0.92f * f),
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
        drawPath(bodyPath, lerp(TorchPalette.BodyDark, TorchPalette.BodyLit, 0.25f + 0.75f * f))
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
            color = TorchPalette.SlotBase.copy(alpha = 0.92f),
            topLeft = Offset(w / 2f - slotW / 2f, slotTop),
            size = Size(slotW, slotH),
            cornerRadius = CornerRadius(slotW / 2f, slotW / 2f),
        )
        val fillH = slotH * f
        if (fillH > 1f) {
            drawRoundRect(
                color = TorchPalette.Highlight.copy(alpha = 0.45f + 0.55f * f),
                topLeft = Offset(w / 2f - slotW / 2f, slotBottom - fillH),
                size = Size(slotW, fillH),
                cornerRadius = CornerRadius(slotW / 2f, slotW / 2f),
            )
        }
    }
}
