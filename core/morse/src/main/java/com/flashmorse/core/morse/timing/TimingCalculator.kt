package com.flashmorse.core.morse.timing

import com.flashmorse.core.model.TimingProfile

/**
 * WPM ↔ 单位时长换算（架构文档 §11.1）。
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
}
