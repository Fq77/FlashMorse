package com.flashmorse.core.morse.timing

import com.flashmorse.core.model.TimingProfile

/**
 * WPM ↔ 单位时长换算（规划文档 §11.1）。
 *
 * 校验：PARIS 标准下一个单词占 50t，因此 10 WPM = 10 × 50 × 120 ms = 60 s。
 */
object TimingCalculator {

    fun unitMillisForWpm(wpm: Int): Int = TimingProfile.unitMillisForWpm(wpm)

    fun wpmForUnitMillis(unitMillis: Int): Int {
        if (unitMillis <= 0) return TimingProfile.MAX_WPM
        return (1200.0 / unitMillis).toInt().coerceIn(TimingProfile.MIN_WPM, TimingProfile.MAX_WPM)
    }

    /** 形如 `10 WPM · 点 120 ms`。 */
    fun summary(timing: TimingProfile): String =
        "${wpmForUnitMillis(timing.unitMillis)} WPM · 点 ${timing.unitMillis} ms"

    /** 形如 `划 360 ms · 字距 360 ms · 词距 840 ms`。 */
    fun detail(timing: TimingProfile): String =
        "划 ${timing.dashMillis} ms · 字距 ${timing.charGapMillis} ms · 词距 ${timing.wordGapMillis} ms"

    /** 估算一段报文的总时长（毫秒），用于发送前提示。 */
    fun estimateMillis(timing: TimingProfile, letterCount: Int, wordCount: Int): Long {
        if (letterCount <= 0) return 0
        // 平均每字符 2.5 个信号元素（ITU 常用近似），此处按精确上下界的中值估算
        val averageSymbolsPerLetter = 2.5
        val signals = letterCount * averageSymbolsPerLetter
        val signalMillis = signals * (timing.unitMillis + timing.dashMillis) / 2.0
        val elementGaps = (signals - letterCount) * timing.elementGapMillis
        val charGaps = (letterCount - wordCount).coerceAtLeast(0) * timing.charGapMillis
        val wordGaps = (wordCount - 1).coerceAtLeast(0) * timing.wordGapMillis
        return (signalMillis + elementGaps + charGaps + wordGaps).toLong()
    }
}
