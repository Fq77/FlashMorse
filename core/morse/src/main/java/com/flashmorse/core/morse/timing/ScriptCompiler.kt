package com.flashmorse.core.morse.timing

import com.flashmorse.core.model.MorseCode
import com.flashmorse.core.model.MorseLetter
import com.flashmorse.core.model.MorseWord
import com.flashmorse.core.model.MorseSymbol
import com.flashmorse.core.model.ScriptCell
import com.flashmorse.core.model.ScriptEvent
import com.flashmorse.core.model.SendScript
import com.flashmorse.core.model.TimingProfile

/**
 * 把 [MorseCode] + [TimingProfile] 编译为绝对时间轴 [SendScript]（规划文档 §6.2）。
 *
 * 关键点：所有事件的时间都是「相对锚点的绝对偏移」，而不是逐元素累加的延迟。
 * 任一事件的抖动不会向后传递，因此累积误差不随时间增长（验收 T-02）。
 */
object ScriptCompiler {

    fun compile(code: MorseCode, timing: TimingProfile): SendScript {
        if (code.isEmpty) return SendScript.Empty.copy(timing = timing)

        val segments = buildSegments(code, timing)

        var elapsedNanos = 0L
        var state = false
        val cells = ArrayList<ScriptCell>(segments.size)
        val events = ArrayList<ScriptEvent>(segments.size + 1)

        for (segment in segments) {
            val index = cells.size
            val durationNanos = segment.durationMillis * NANOS_PER_MILLI
            cells += ScriptCell(
                index = index,
                symbol = segment.symbol,
                letterIndex = segment.letterIndex,
                startNanos = elapsedNanos,
                endNanos = elapsedNanos + durationNanos,
            )
            if (segment.on != state) {
                events += ScriptEvent(offsetNanos = elapsedNanos, on = segment.on, cellIndex = index)
                state = segment.on
            }
            elapsedNanos += durationNanos
        }

        // 保证脚本以熄灯结束：最后一段必为信号元素，因此在末尾补一次关灯事件。
        if (state) {
            events += ScriptEvent(
                offsetNanos = elapsedNanos,
                on = false,
                cellIndex = cells.lastIndex.coerceAtLeast(0),
            )
        }

        return SendScript(
            code = code,
            timing = timing,
            events = events,
            cells = cells,
            totalNanos = elapsedNanos,
        )
    }

    /** 单个字符的时间轴（对照表试闪用）。 */
    fun compileSingle(
        char: Char,
        pattern: String,
        timing: TimingProfile,
    ): SendScript = compile(
        code = MorseCode(listOf(MorseWord(listOf(MorseLetter(char, pattern))))),
        timing = timing,
    )

    private fun buildSegments(code: MorseCode, timing: TimingProfile): List<Segment> {
        val segments = ArrayList<Segment>(code.letters.size * 4)
        var letterIndex = 0

        code.words.forEachIndexed { wordIndex, word ->
            word.letters.forEachIndexed { indexInWord, letter ->
                letter.symbols.forEachIndexed { symbolIndex, symbol ->
                    val duration = when (symbol) {
                        MorseSymbol.Dot -> timing.unitMillis
                        MorseSymbol.Dash -> timing.dashMillis
                        else -> 0
                    }
                    segments += Segment(on = true, durationMillis = duration, symbol = symbol, letterIndex = letterIndex)
                    if (symbolIndex != letter.symbols.lastIndex) {
                        segments += Segment(
                            on = false,
                            durationMillis = timing.elementGapMillis,
                            symbol = MorseSymbol.ElementGap,
                            letterIndex = letterIndex,
                        )
                    }
                }
                // 字符间隔：同一单词内相邻字母之间
                if (indexInWord != word.letters.lastIndex) {
                    segments += Segment(
                        on = false,
                        durationMillis = timing.charGapMillis,
                        symbol = MorseSymbol.CharGap,
                        letterIndex = letterIndex,
                    )
                }
                letterIndex++
            }
            // 单词间隔：相邻单词之间
            if (wordIndex != code.words.lastIndex) {
                segments += Segment(
                    on = false,
                    durationMillis = timing.wordGapMillis,
                    symbol = MorseSymbol.WordGap,
                    letterIndex = (letterIndex - 1).coerceAtLeast(0),
                )
            }
        }

        return segments
    }

    private data class Segment(
        val on: Boolean,
        val durationMillis: Int,
        val symbol: MorseSymbol,
        val letterIndex: Int,
    )

    private const val NANOS_PER_MILLI = 1_000_000L
}
