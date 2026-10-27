package com.flashmorse.core.model

/**
 * 时序参数，遵循 ITU-R M.1677-1：
 * 点 1t、划 3t、元素间隔 1t、字符间隔 3t、单词间隔 7t。
 *
 * 单位时长 `t = 1200 / WPM`（毫秒，PARIS 标准）。
 *
 * @param unitMillis 单位时长 t（毫秒）
 * @param compensationMillis 实测的「指令下发 → 实际发光」延迟 L_cmd。
 *   调度器以该值后移时间轴锚点，使光的实际出现时刻与逻辑时间轴对齐，
 *   同时保证各元素时长（点划比例）不被改变。
 */
data class TimingProfile(
    val unitMillis: Int,
    val dashUnits: Int = DASH_UNITS,
    val elementGapUnits: Int = ELEMENT_GAP_UNITS,
    val charGapUnits: Int = CHAR_GAP_UNITS,
    val wordGapUnits: Int = WORD_GAP_UNITS,
    val compensationMillis: Int = 0,
) {
    init {
        require(unitMillis > 0) { "unitMillis must be positive" }
        require(dashUnits > 0 && elementGapUnits > 0 && charGapUnits > 0 && wordGapUnits > 0) {
            "gap multipliers must be positive"
        }
    }

    val dashMillis: Int get() = unitMillis * dashUnits
    val elementGapMillis: Int get() = unitMillis * elementGapUnits
    val charGapMillis: Int get() = unitMillis * charGapUnits
    val wordGapMillis: Int get() = unitMillis * wordGapUnits

    /** LED 通道的可靠上限（架构文档 §6.1）：15 WPM 以上单位时长 < 80 ms，HAL 延迟不可忽略。 */
    val isLedReliable: Boolean get() = unitMillis >= LED_RELIABLE_UNIT_MILLIS

    fun withUnitMillis(unitMillis: Int): TimingProfile = copy(unitMillis = unitMillis)

    fun withCompensation(compensationMillis: Int): TimingProfile =
        copy(compensationMillis = compensationMillis)

    companion object {
        const val DASH_UNITS = 3
        const val ELEMENT_GAP_UNITS = 1
        const val CHAR_GAP_UNITS = 3
        const val WORD_GAP_UNITS = 7

        /** LED 通道的可靠上限：单位时长 >= 80 ms（即 <= 15 WPM）。 */
        const val LED_RELIABLE_UNIT_MILLIS = 80

        const val MIN_WPM = 5
        const val MAX_WPM = 25
        const val DEFAULT_WPM = 10

        /** 由 WPM 计算单位时长，保留整数毫秒。 */
        fun unitMillisForWpm(wpm: Int): Int {
            val clamped = wpm.coerceIn(MIN_WPM, MAX_WPM)
            return Math.round(1200.0 / clamped).toInt()
        }

        fun forWpm(wpm: Int, compensationMillis: Int = 0): TimingProfile =
            TimingProfile(
                unitMillis = unitMillisForWpm(wpm),
                compensationMillis = compensationMillis,
            )
    }
}
