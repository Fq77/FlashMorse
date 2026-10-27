package com.flashmorse.core.morse

import com.flashmorse.core.model.MorseSymbol
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.morse.codec.MorseEncoder
import com.flashmorse.core.morse.codec.MorseTable
import com.flashmorse.core.morse.codec.TextNormalizer
import com.flashmorse.core.morse.timing.ScriptCompiler
import com.flashmorse.core.morse.timing.TimingCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 编码器 + 时间轴编译（验收 A-01 / A-02 / T-02 / T-03）。 */
class MorseEncoderTest {

    private val encoder = MorseEncoder()

    @Test
    fun `encode hello world`() {
        val code = encoder.encode("HELLO WORLD")
        assertEquals(2, code.words.size)
        assertEquals(10, code.letterCount)
        assertEquals("HELLO WORLD", code.plainText)
        assertEquals("....", code.words[0].letters[0].pattern) // H
        assertEquals(".-..", code.words[0].letters[2].pattern) // L
    }

    @Test
    fun `lowercase input is upper cased`() {
        val code = encoder.encode("sos")
        assertEquals("SOS", code.plainText)
    }

    @Test
    fun `unknown chars are preserved as question marks instead of being dropped`() {
        val code = encoder.encode("A中B")
        assertEquals("A?B", code.plainText)
        assertEquals("..--..", code.letters[1].pattern)
    }

    @Test
    fun `whitespace is collapsed`() {
        val code = encoder.encode("  A   B  ")
        assertEquals(2, code.words.size)
        assertEquals("A B", code.plainText)
    }

    @Test
    fun `blank input yields empty code`() {
        assertTrue(encoder.encode("   ").isEmpty)
    }

    @Test
    fun `timing follows ITU multipliers`() {
        val timing = TimingProfile.forWpm(10)
        assertEquals(120, timing.unitMillis)
        assertEquals(360, timing.dashMillis)
        assertEquals(120, timing.elementGapMillis)
        assertEquals(360, timing.charGapMillis)
        assertEquals(840, timing.wordGapMillis)
    }

    @Test
    fun `sos total duration is 27 units`() {
        // 架构文档 §11.1：SOS 在 10 WPM 下总时长 = 27t = 3240 ms
        val timing = TimingProfile.forWpm(10)
        val script = ScriptCompiler.compile(encoder.encode("SOS"), timing)
        assertEquals(3240L, script.totalMillis)
        assertEquals(9, script.cells.count { it.symbol == MorseSymbol.Dot || it.symbol == MorseSymbol.Dash })
        assertEquals(6, script.cells.count { it.symbol == MorseSymbol.ElementGap })
        assertEquals(2, script.cells.count { it.symbol == MorseSymbol.CharGap })
    }

    @Test
    fun `dash duration is exactly three times dot duration`() {
        val timing = TimingProfile.forWpm(10)
        val script = ScriptCompiler.compile(encoder.encode("A"), timing)
        val dot = script.cells.first { it.symbol == MorseSymbol.Dot }
        val dash = script.cells.first { it.symbol == MorseSymbol.Dash }
        assertEquals(3.0, dash.durationNanos.toDouble() / dot.durationNanos.toDouble(), 1e-9)
    }

    @Test
    fun `events alternate starting with on and end with off`() {
        val script = ScriptCompiler.compile(encoder.encode("HELLO"), TimingProfile.forWpm(10))
        assertTrue(script.events.isNotEmpty())
        assertEquals(0L, script.events.first().offsetNanos)
        assertTrue(script.events.first().on)
        assertFalse(script.events.last().on)
        assertEquals(script.totalNanos, script.events.last().offsetNanos)

        var previous = false
        script.events.forEach { event ->
            assertTrue("events must alternate", event.on != previous)
            previous = event.on
        }
    }

    @Test
    fun `events are monotonic and never accumulate drift`() {
        val script = ScriptCompiler.compile(
            encoder.encode("THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG"),
            TimingProfile.forWpm(25),
        )
        var last = -1L
        script.events.forEach { event ->
            assertTrue(event.offsetNanos > last)
            last = event.offsetNanos
        }
        // 第 200 个事件与首事件的间隔仍等于纯单位时长之和，说明不存在累积误差
        assertEquals(script.totalNanos, script.cells.sumOf { it.durationNanos })
    }

    @Test
    fun `word gap is seven units`() {
        val timing = TimingProfile.forWpm(10)
        val script = ScriptCompiler.compile(encoder.encode("A B"), timing)
        val wordGap = script.cells.single { it.symbol == MorseSymbol.WordGap }
        assertEquals(840L, wordGap.durationNanos / 1_000_000L)
    }

    @Test
    fun `wpm conversion is reversible at canonical points`() {
        assertEquals(120, TimingCalculator.unitMillisForWpm(10))
        assertEquals(240, TimingCalculator.unitMillisForWpm(5))
        assertEquals(48, TimingCalculator.unitMillisForWpm(25))
        assertEquals(10, TimingCalculator.wpmForUnitMillis(120))
        assertEquals("10 WPM · 点 120 ms", TimingCalculator.summary(TimingProfile.forWpm(10)))
    }

    @Test
    fun `normalizer keeps supported punctuation`() {
        val text = TextNormalizer.normalize("hi, ok?")
        assertEquals("HI, OK?", text)
        MorseTable.supportedChars.forEach { char ->
            assertEquals(char, TextNormalizer.mapChar(char))
        }
    }
}
