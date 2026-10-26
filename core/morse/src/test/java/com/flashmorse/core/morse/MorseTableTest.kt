package com.flashmorse.core.morse

import com.flashmorse.core.morse.codec.MorseTable
import com.flashmorse.core.morse.codec.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ITU-R M.1677-1 码表校验（验收 A-01）。 */
class MorseTableTest {

    @Test
    fun `alphabet matches ITU table`() {
        val expected = mapOf(
            'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
            'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
            'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
            'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
            'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
            'Z' to "--..",
        )
        expected.forEach { (char, pattern) ->
            assertEquals("char $char", pattern, MorseTable.patternOf(char))
        }
    }

    @Test
    fun `digits match ITU table`() {
        assertEquals("-----", MorseTable.patternOf('0'))
        assertEquals(".----", MorseTable.patternOf('1'))
        assertEquals("..---", MorseTable.patternOf('2'))
        assertEquals("...--", MorseTable.patternOf('3'))
        assertEquals("....-", MorseTable.patternOf('4'))
        assertEquals(".....", MorseTable.patternOf('5'))
        assertEquals("-....", MorseTable.patternOf('6'))
        assertEquals("--...", MorseTable.patternOf('7'))
        assertEquals("---..", MorseTable.patternOf('8'))
        assertEquals("----.", MorseTable.patternOf('9'))
    }

    @Test
    fun `punctuation matches ITU table`() {
        assertEquals(".-.-.-", MorseTable.patternOf('.'))
        assertEquals("--..--", MorseTable.patternOf(','))
        assertEquals("..--..", MorseTable.patternOf('?'))
        assertEquals("-..-.", MorseTable.patternOf('/'))
        assertEquals(".----.", MorseTable.patternOf('\''))
        assertEquals("-.-.--", MorseTable.patternOf('!'))
        assertEquals("-.--.", MorseTable.patternOf('('))
        assertEquals("-.--.-", MorseTable.patternOf(')'))
        assertEquals(".-...", MorseTable.patternOf('&'))
        assertEquals("---...", MorseTable.patternOf(':'))
        assertEquals("-.-.-.", MorseTable.patternOf(';'))
        assertEquals("-...-", MorseTable.patternOf('='))
        assertEquals(".-.-.", MorseTable.patternOf('+'))
        assertEquals("-....-", MorseTable.patternOf('-'))
        assertEquals("..--.-", MorseTable.patternOf('_'))
        assertEquals(".-..-.", MorseTable.patternOf('"'))
        assertEquals("...-..-", MorseTable.patternOf('$'))
        assertEquals(".--.-.", MorseTable.patternOf('@'))
    }

    @Test
    fun `reverse lookup round trips`() {
        assertTrue(MorseTable.supportedChars.isNotEmpty())
        MorseTable.supportedChars.forEach { char ->
            val pattern = MorseTable.patternOf(char)!!
            assertEquals(char, MorseTable.charFor(pattern))
        }
        assertEquals(54, MorseTable.supportedChars.size)
    }

    @Test
    fun `unknown char has no pattern`() {
        assertNull(MorseTable.patternOf('中'))
        assertNull(MorseTable.charFor("......"))
    }

    @Test
    fun `normalizer replaces unsupported with question mark`() {
        assertEquals("?", TextNormalizer.mapChar('中').toString())
        assertEquals("A", TextNormalizer.mapChar('ａ').toString())
        assertEquals(",", TextNormalizer.mapChar('，').toString())
        assertEquals(".", TextNormalizer.mapChar('。').toString())
    }
}
