package com.flashmorse.core.model

/**
 * 时间轴上的一个灯态翻转事件。
 *
 * [offsetNanos] 是相对锚点的绝对偏移——任一事件的抖动不会向后传递，
 * 这是与「逐条 delay() 累积」的本质区别（规划文档 §6.2）。
 */
data class ScriptEvent(
    val offsetNanos: Long,
    val on: Boolean,
    /** 触发该事件的符号单元索引（对应 [SendScript.cells]）。 */
    val cellIndex: Int,
)

/**
 * 供 UI 高亮的符号单元。点/划与间隔都是单元，等宽渲染后进度可精确对应到像素。
 */
data class ScriptCell(
    val index: Int,
    val symbol: MorseSymbol,
    /** 所属字母在 [SendScript.code] 中的索引；间隔单元归属其前面的字母。 */
    val letterIndex: Int,
    val startNanos: Long,
    val endNanos: Long,
) {
    val durationNanos: Long get() = endNanos - startNanos
}

/**
 * 预编译的绝对时间轴（规划文档 §5.3）。
 *
 * 播放器只需按 [events] 在绝对时刻切换灯态，不关心底层是 LED 还是屏幕。
 */
data class SendScript(
    val code: MorseCode,
    val timing: TimingProfile,
    val events: List<ScriptEvent>,
    val cells: List<ScriptCell>,
    val totalNanos: Long,
) {
    val totalMillis: Long get() = totalNanos / 1_000_000L

    val isEmpty: Boolean get() = events.isEmpty()

    /** 给定已播放的纳秒数，返回当前单元索引。 */
    fun cellIndexAt(elapsedNanos: Long): Int {
        if (cells.isEmpty()) return -1
        val clamped = elapsedNanos.coerceAtLeast(0L)
        var low = 0
        var high = cells.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cell = cells[mid]
            when {
                clamped < cell.startNanos -> high = mid - 1
                clamped >= cell.endNanos -> low = mid + 1
                else -> return mid
            }
        }
        return if (clamped >= cells.last().endNanos) cells.lastIndex else low.coerceIn(0, cells.lastIndex)
    }

    companion object {
        val Empty = SendScript(
            code = MorseCode.Empty,
            timing = TimingProfile.forWpm(TimingProfile.DEFAULT_WPM),
            events = emptyList(),
            cells = emptyList(),
            totalNanos = 0L,
        )
    }
}
