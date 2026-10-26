package com.flashmorse.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
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
        val pxToDp = with(LocalDensity.current) { 1f / density }
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
            Modifier.panHorizontally { dx ->
                panDp = (panDp - dx * pxToDp)
                    .coerceIn(0f, (widths.sum() - availableDp).coerceAtLeast(0f))
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
 * 水平平移手势（像素增量，向左拖为负）。
 *
 * 在 **Initial pass** 拦截水平拖动：组件常嵌在可点击 Card 与 HorizontalPager 里，
 * Main pass 的拖动会被分页手势抢走（表现为"滚动不生效"）；Initial 拦截让本
 * 手势先于分页拿到事件。垂直位移先超过阈值的拖动不接管（仍交给列表/分页）。
 */
internal fun Modifier.panHorizontally(onDrag: (Float) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var dragging = false
            var lastX = down.position.x
            var lastY = down.position.y
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dx = change.position.x - lastX
                val dy = change.position.y - lastY
                lastX = change.position.x
                lastY = change.position.y
                if (!dragging) {
                    if (kotlin.math.abs(dy) > viewConfiguration.touchSlop) break   // 垂直意图：放弃
                    if (kotlin.math.abs(dx) > viewConfiguration.touchSlop) dragging = true
                }
                if (dragging) {
                    change.consume()
                    onDrag(dx)
                }
            }
        }
    }

/**
 * 按绘制规则计算一个 pattern 需要的总宽度（与绘制逻辑一致：点 1、划 3、
 * 每个符号后 0.9 倍间距）。
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

/**
 * 单个字符的点划模式，供对照表、短语行与发送卡复用。
 *
 * [scrollable] 时溢出可横向拖动查看：**不使用 scrollable modifier**——
 * 它嵌在可点击的 Card 里时拖动经常被点击手势吃掉，改为自绘 pan
 * （与 [MorseStream] 空闲态同一套手势），行为稳定。
 * [centered] 决定未溢出时的对齐（对照表要居中，短语行要跟标题左对齐）。
 */
@Composable
fun MorsePattern(
    pattern: String,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    scrollable: Boolean = false,
    centered: Boolean = false,
) {
    if (!scrollable) {
        Canvas(modifier = modifier) {
            drawMorsePattern(pattern, height.toPx(), color)
        }
        return
    }

    BoxWithConstraints(modifier) {
        val needed = morsePatternWidth(pattern, height)
        if (needed <= maxWidth) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart,
            ) {
                Canvas(modifier = Modifier.width(needed).height(height)) {
                    drawMorsePattern(pattern, height.toPx(), color)
                }
            }
            return@BoxWithConstraints
        }

        // 溢出：视口固定，内容整体平移，拖多少看多少
        var panPx by remember(pattern) { mutableFloatStateOf(0f) }
        val maxPanPx = with(LocalDensity.current) { (needed - maxWidth).toPx() }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .panHorizontally { dx ->
                    panPx = (panPx - dx).coerceIn(0f, maxPanPx)
                },
        ) {
            translate(left = -panPx) {
                drawMorsePattern(pattern, height.toPx(), color)
            }
        }
    }
}

/** MorsePattern 的实际绘制：横轴按点 1 / 划 3 / 间距 0.9 递进。 */
private fun DrawScope.drawMorsePattern(
    pattern: String,
    barPx: Float,
    color: Color,
) {
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
