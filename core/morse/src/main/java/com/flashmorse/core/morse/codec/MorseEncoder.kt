package com.flashmorse.core.morse.codec

import com.flashmorse.core.model.MorseCode
import com.flashmorse.core.model.MorseLetter
import com.flashmorse.core.model.MorseWord

/**
 * 文本 → [MorseCode]。
 *
 * 编码器只负责语义层结构（单词 / 字母），时间与间隔由
 * [com.flashmorse.core.morse.timing.ScriptCompiler] 负责，二者解耦便于单测。
 */
class MorseEncoder {

    fun encode(raw: String): MorseCode = encodeNormalized(TextNormalizer.normalize(raw))

    /** 输入必须已归一化（[TextNormalizer.normalize]）。 */
    fun encodeNormalized(normalized: String): MorseCode {
        if (normalized.isBlank()) return MorseCode.Empty
        val words = normalized.split(' ').filter { it.isNotEmpty() }.map { word ->
            MorseWord(word.map { char -> MorseLetter(char, MorseTable.patternOf(char) ?: "..--..") })
        }
        return MorseCode(words)
    }
}
