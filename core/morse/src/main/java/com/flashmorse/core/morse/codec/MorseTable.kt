package com.flashmorse.core.morse.codec

import com.flashmorse.core.model.MorseLetter

/**
 * ITU-R M.1677-1 国际摩斯码表。
 *
 * 码表是编码、解码与对照表的唯一事实来源，任何字符都必须能在此查到。
 */
object MorseTable {

    private val patterns: Map<Char, String> = linkedMapOf(
        'A' to ".-",
        'B' to "-...",
        'C' to "-.-.",
        'D' to "-..",
        'E' to ".",
        'F' to "..-.",
        'G' to "--.",
        'H' to "....",
        'I' to "..",
        'J' to ".---",
        'K' to "-.-",
        'L' to ".-..",
        'M' to "--",
        'N' to "-.",
        'O' to "---",
        'P' to ".--.",
        'Q' to "--.-",
        'R' to ".-.",
        'S' to "...",
        'T' to "-",
        'U' to "..-",
        'V' to "...-",
        'W' to ".--",
        'X' to "-..-",
        'Y' to "-.--",
        'Z' to "--..",
        '0' to "-----",
        '1' to ".----",
        '2' to "..---",
        '3' to "...--",
        '4' to "....-",
        '5' to ".....",
        '6' to "-....",
        '7' to "--...",
        '8' to "---..",
        '9' to "----.",
        '.' to ".-.-.-",
        ',' to "--..--",
        '?' to "..--..",
        '\'' to ".----.",
        '!' to "-.-.--",
        '/' to "-..-.",
        '(' to "-.--.",
        ')' to "-.--.-",
        '&' to ".-...",
        ':' to "---...",
        ';' to "-.-.-.",
        '=' to "-...-",
        '+' to ".-.-.",
        '-' to "-....-",
        '_' to "..--.-",
        '"' to ".-..-.",
        '$' to "...-..-",
        '@' to ".--.-.",
    )

    private val reverse: Map<String, Char> = patterns.entries.associate { (c, p) -> p to c }

    /** 全部可编码字符，按码表顺序（字母、数字、标点）。 */
    val letters: List<MorseLetter> = patterns.keys.map { MorseLetter(it, patterns.getValue(it)) }

    val alphabet: List<MorseLetter> = letters.filter { it.char in 'A'..'Z' }
    val digits: List<MorseLetter> = letters.filter { it.char in '0'..'9' }
    val punctuation: List<MorseLetter> = letters.filter { it.char !in 'A'..'Z' && it.char !in '0'..'9' }

    fun patternOf(char: Char): String? = patterns[char]

    fun letterFor(char: Char): MorseLetter? = patterns[char]?.let { MorseLetter(char, it) }

    fun charFor(pattern: String): Char? = reverse[pattern]

    fun supports(char: Char): Boolean = patterns.containsKey(char)

    val supportedChars: Set<Char> get() = patterns.keys
}
