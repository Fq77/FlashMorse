package com.flashmorse.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flashmorse.core.model.MorseSymbol
import com.flashmorse.core.model.ScriptCell
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 符号流视图（规划文档 §7.5 第 2 段）。
 *
 * 直接 `Canvas` 绘制而不是逐符号创建 Composable：发送中每 ~120 ms 一次状态刷新，
 * 逐元素组合会带来不必要的重组开销。
 *
 * 视图采用「窗口化」渲染——只绘制当前播放位置附近的符号段落，长报文也不会把
 * 符号压成噪点；已发部分高亮、未发部分降低不透明度，当前符号使用主色。
 */
@Composable
fun MorseStream(
    cells: List<ScriptCell>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 7.dp,
) {
    val colorScheme = MiuixTheme.colorScheme
    val activeColor = colorScheme.primary
    val sentColor = colorScheme.onBackground
    val pendingColor = colorScheme.onBackgroundVariant.copy(alpha = 0.28f)

    val metrics = rememberStreamMetrics(barHeight)
    val widths = remember(cells, metrics) { cells.map { metrics.widthOf(it.symbol) } }

    BoxWithConstraints(modifier = modifier) {
        val availableDp = maxWidth.value
        // 空闲时允许手动横向拖动（报文长于视口时 otherwise 看不到尾部）；
        // 发送中切回自动跟随，拖动偏移作废。
        var panDp by remember(cells) { mutableFloatStateOf(0f) }
        val autoFollow = activeIndex >= 0
        val target = if (autoFollow) {
            computeStart(widths, activeIndex, availableDp)
        } else {
            panDp.coerceIn(0f, (widths.sum() - availableDp).coerceAtLeast(0f))
        }
        val startX by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 140),
            label = "morseStreamScroll",
        )



        val gesture = if (!autoFollow) {
            Modifier.pointerInput(cells, availableDp) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    panDp = (panDp - dragAmount / density)
                        .coerceIn(0f, (widths.sum() - availableDp).coerceAtLeast(0f))
                }
            }
        } else {
            Modifier
        }

        Canvas(modifier = Modifier.fillMaxSize().then(gesture)) {
            val barPx = metrics.barHeight.toPx()
            val radius = CornerRadius(barPx / 2f, barPx / 2f)
            val centerY = size.height / 2f
            var xDp = -startX

            for (index in cells.indices) {
                val width = widths[index]
                if (xDp + width > 0f && xDp < availableDp) {
                    val symbol = cells[index].symbol
                    val color = when {
                        index == activeIndex -> activeColor
                        index < activeIndex -> sentColor
                        else -> pendingColor
                    }
                    val x = xDp.dp.toPx()
                    when (symbol) {
                        MorseSymbol.Dot -> drawCircle(
                            color = color,
                            radius = barPx / 2f,
                            center = Offset(x + (width.dp.toPx() / 2f), centerY),
                        )

                        MorseSymbol.Dash -> drawRoundRect(
                            color = color,
                            topLeft = Offset(x, centerY - barPx / 2f),
                            size = Size(width.dp.toPx(), barPx),
                            cornerRadius = radius,
                        )

                        else -> Unit // 间隔不绘制
                    }
                }
                if (xDp > availableDp) break
                xDp += width
            }
        }
    }
}

/**
 * 按绘制规则计算一个 pattern 需要的总宽度（与 [MorsePattern] 内部一致：
 * 点 1、划 3、每个符号后 0.9 倍间距）。
 */
fun morsePatternWidth(pattern: String, barHeight: Dp): Dp {
    var units = 0f
    pattern.forEach { ch ->
        when (ch) {
            '.' -> units += 1.9f
            '-' -> units += 3.9f
        }
    }
    return barHeight * units
}

/** 单个字符的点划模式，供对照表、短语行与发送卡复用。 */
@Composable
fun MorsePattern(
    pattern: String,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    /** 溢出时是否允许横向滚动（false = 超出部分被裁掉）。 */
    scrollable: Boolean = false,
) {
    if (scrollable) {
        BoxWithConstraints(modifier) {
            val needed = morsePatternWidth(pattern, height)
            if (needed > maxWidth) {
                // 内容比视口宽：视口保持给定宽度，内容按实际宽度绘制并可拖动查看
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MorsePattern(
                        pattern = pattern,
                        modifier = Modifier
                            .width(needed)
                            .height(height),
                        height = height,
                        color = color,
                    )
                }
            } else {
                MorsePattern(
                    pattern = pattern,
                    modifier = Modifier
                        .fillMaxSize()
                        .height(height),
                    height = height,
                    color = color,
                )
            }
        }
        return
    }

    Canvas(modifier = modifier) {
        val barPx = height.toPx()
        val dotW = barPx
        val dashW = barPx * 3f
        val gap = barPx * 0.9f
        val centerY = size.height / 2f
        var x = 0f
        pattern.forEach { ch ->
            when (ch) {
                '.' -> {
                    drawCircle(color = color, radius = barPx / 2f, center = Offset(x + dotW / 2f, centerY))
                    x += dotW + gap
                }

                '-' -> {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, centerY - barPx / 2f),
                        size = Size(dashW, barPx),
                        cornerRadius = CornerRadius(barPx / 2f, barPx / 2f),
                    )
                    x += dashW + gap
                }
            }
        }
    }
}

@Composable
private fun rememberStreamMetrics(barHeight: Dp): StreamMetrics =
    remember(barHeight) { StreamMetrics(barHeight) }

private class StreamMetrics(val barHeight: Dp) {
    private val dotWidth = barHeight + 1.dp
    private val dashWidth = (barHeight + 1.dp) * 3
    private val elementGap = barHeight * 0.75f
    private val charGap = elementGap * 3
    private val wordGap = elementGap * 7

    fun widthOf(symbol: MorseSymbol): Float = when (symbol) {
        MorseSymbol.Dot -> dotWidth.value
        MorseSymbol.Dash -> dashWidth.value
        MorseSymbol.ElementGap -> elementGap.value
        MorseSymbol.CharGap -> charGap.value
        MorseSymbol.WordGap -> wordGap.value
    }
}

/** 让当前符号稳定落在视口左侧 45% 处，右侧始终能看到即将发送的内容。 */
private fun computeStart(
    widths: List<Float>,
    activeIndex: Int,
    availableDp: Float,
): Float {
    if (widths.isEmpty()) return 0f
    val total = widths.sum()
    if (total <= availableDp) return 0f
    val index = activeIndex.coerceIn(0, widths.lastIndex)
    var cumulative = 0f
    for (i in 0 until index) cumulative += widths[i]
    val target = cumulative - availableDp * 0.45f
    return target.coerceIn(0f, total - availableDp)
}
