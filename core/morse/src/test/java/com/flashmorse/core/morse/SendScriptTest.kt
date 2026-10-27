package com.flashmorse.core.morse

import com.flashmorse.core.model.MorseSymbol
import com.flashmorse.core.model.SendScript
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.morse.codec.MorseDecoder
import com.flashmorse.core.morse.codec.MorseEncoder
import com.flashmorse.core.morse.timing.ScriptCompiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 时间轴查询与解码闭环。
 *
 * 这一层是调度器与 UI 共用的唯一真相源：调度器按 events 下发灯态，
 * 进度环按 cells 外推位置，两侧都依赖 [SendScript.cellIndexAt] 的边界行为。
 * 边界错一格，表现就是「灯在闪，进度条慢一拍」这种最难查的观感问题。
 */
class SendScriptTest {

    private val encoder = MorseEncoder()
    private val timing = TimingProfile.forWpm(10)

    @Test
    fun `empty script has no cell at any time`() {
        assertEquals(-1, SendScript.Empty.cellIndexAt(0))
        assertEquals(-1, SendScript.Empty.cellIndexAt(1_000_000_000L))
    }

    @Test
    fun `cellIndexAt clamps both ends instead of returning out of range`() {
        val script = ScriptCompiler.compile(encoder.encode("SOS"), timing)

        assertEquals(0, script.cellIndexAt(-1))
        assertEquals(0, script.cellIndexAt(0))
        assertEquals(script.cells.lastIndex, script.cellIndexAt(script.totalNanos))
        assertEquals(script.cells.lastIndex, script.cellIndexAt(script.totalNanos + 10_000_000L))
    }

    @Test
    fun `every sample inside a cell maps back to that cell`() {
        val script = ScriptCompiler.compile(encoder.encode("PARIS"), timing)
        assertTrue(script.cells.isNotEmpty())

        script.cells.forEach { cell ->
            assertEquals(cell.index, script.cellIndexAt(cell.startNanos))
            assertEquals(cell.index, script.cellIndexAt(cell.endNanos - 1))
        }
    }

    @Test
    fun `compiling the same input twice yields an identical timeline`() {
        // 编译必须是纯函数：调度器循环重播时会再次编译，任何隐藏状态都会变成累积漂移
        val first = ScriptCompiler.compile(encoder.encode("CQ CQ DE BD1ABC"), timing)
        val second = ScriptCompiler.compile(encoder.encode("CQ CQ DE BD1ABC"), timing)

        assertEquals(first.totalNanos, second.totalNanos)
        assertEquals(first.events, second.events)
        assertEquals(first.cells, second.cells)
    }

    @Test
    fun `compileSingle produces one character worth of units`() {
        // A = 点划（1t + 1t 间隔 + 3t）= 5t
        val script = ScriptCompiler.compileSingle('A', ".-", timing)
        assertEquals(timing.unitMillis * 5L, script.totalMillis)
        assertEquals(1, script.code.letterCount)
    }

    @Test
    fun `decoding the compiled timeline round trips to the original text`() {
        val text = "HELLO WORLD"
        val script = ScriptCompiler.compile(encoder.encode(text), timing)
        val decoded = MorseDecoder.decodeSymbols(script.cells.map { it.symbol })
        assertEquals(text, decoded)
    }

    @Test
    fun `element gap must not split a character`() {
        // H = .... ，内部三个 1t 间隔不能把字符切成四个 E
        val script = ScriptCompiler.compile(encoder.encode("H"), timing)
        val decoded = MorseDecoder.decodeSymbols(script.cells.map { it.symbol })
        assertEquals("H", decoded)
    }

    @Test
    fun `word gap is decoded as a single space`() {
        val script = ScriptCompiler.compile(encoder.encode("E E"), timing)
        val decoded = MorseDecoder.decodeSymbols(script.cells.map { it.symbol })
        assertEquals("E E", decoded)
        assertEquals(1, decoded.count { it == ' ' })
    }

    @Test
    fun `dash cells are exactly three times dot cells at every speed`() {
        listOf(5, 10, 15, 20, 25).forEach { wpm ->
            val script = ScriptCompiler.compile(encoder.encode("A"), TimingProfile.forWpm(wpm))
            val dot = script.cells.first { it.symbol == MorseSymbol.Dot }
            val dash = script.cells.first { it.symbol == MorseSymbol.Dash }
            assertEquals(
                "dash/dot ratio at $wpm WPM",
                3.0,
                dash.durationNanos.toDouble() / dot.durationNanos.toDouble(),
                1e-9,
            )
        }
    }
}
