package com.flashmorse.core.model

import kotlin.math.roundToInt

/**
 * 亮度模型：界面用百分比，通道用 0..1 的比例。
 *
 * 存百分比而不是硬件档位有两个原因：
 * 1. 屏幕通道没有"档位"概念，LED 通道的档位数又随机型变化（本机 100 级，有的机型只有 1 级），
 *    百分比是唯一跨通道、跨设备都成立的单位；
 * 2. 用户换机后设置仍然可解释——"我要 40% 亮度"，而不是"我要第 37 档"。
 */
object Brightness {

    /**
     * 硬件档位的感知补偿指数。
     *
     * 档位≈驱动电流≈光通量（线性），而人眼对亮度的感知接近幂函数（Stevens 指数 <1），
     * 若把百分比直接当档位，低段会"一滑就暗到底"、高段几乎没有变化。
     * 用 γ=2.2 做反向补偿后，滑杆的每一段位移对应接近等量的主观变化。
     */
    const val GAMMA = 2.2f

    /** 下限不是 1%：低于 10% 时 LED 驱动电流过小、边沿明显变软，已不适合作为信号。 */
    const val MIN_PERCENT = 10

    const val MAX_PERCENT = 100

    const val DEFAULT_PERCENT = MAX_PERCENT

    /**
     * 低于该比例时，LED 驱动电流小、上升/下降沿明显变软，接收端判读余量下降。
     * 界面据此给出「仅建议近距离使用」的提示，而不是禁止。
     */
    const val LOW_THRESHOLD_PERCENT = 40

    fun clampPercent(percent: Int): Int = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)

    fun percentToFraction(percent: Int): Float = clampPercent(percent) / 100f

    // 必须四舍五入而不是截断：0.29f * 100f = 28.999998，截断会平白丢掉 1%。
    fun fractionToPercent(fraction: Float): Int =
        clampPercent((fraction * 100f).roundToInt())

    /**
     * 比例 → 硬件档位。
     *
     * 设备上报的档位范围是 `[1, maxLevel]`（1 为最暗），这里保证：
     * 端点严格对齐（0 → 1、1 → max），中间单调不降，且任何输入都不越界。
     */
    fun levelFor(
        fraction: Float,
        maxLevel: Int,
        minLevel: Int = 1,
    ): Int {
        if (maxLevel <= minLevel) return maxLevel.coerceAtLeast(1)
        val normalized = fraction.coerceIn(0f, 1f)
        val curved = Math.pow(normalized.toDouble(), GAMMA.toDouble())
        val span = maxLevel - minLevel
        return (minLevel + span * curved).toInt().coerceIn(minLevel, maxLevel)
    }
}
