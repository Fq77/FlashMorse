package com.flashmorse.core.morse.codec

import com.flashmorse.core.model.MorseSymbol
import com.flashmorse.core.model.isSignal

/**
 * 符号/模式 → 文本，供手动发报模式的回显使用（规划文档 §5.3）。
 */
object MorseDecoder {

    /** 一组点划模式解码为字符，未知模式返回 `?`。 */
    fun charFor(pattern: String): Char = MorseTable.charFor(pattern) ?: '?'

    /** 按模式序列解码为文本，`" "` 表示单词分隔。 */
    fun decode(patterns: List<String>): String {
        val sb = StringBuilder()
        for (pattern in patterns) {
            if (pattern.isEmpty()) continue
            if (pattern == WORD_BREAK) sb.append(' ') else sb.append(charFor(pattern))
        }
        return sb.toString()
    }

    /** 从元素序列解码（间隔元素触发断字/断词）。 */
    fun decodeSymbols(symbols: List<MorseSymbol>): String {
        val patterns = mutableListOf<String>()
        val current = StringBuilder()
        for (symbol in symbols) {
            when {
                symbol.isSignal -> current.append(if (symbol == MorseSymbol.Dot) '.' else '-')
                else -> {
                    if (current.isNotEmpty()) {
                        patterns += current.toString()
                        current.clear()
                    }
                    if (symbol == MorseSymbol.WordGap) patterns += WORD_BREAK
                }
            }
        }
        if (current.isNotEmpty()) patterns += current.toString()
        return decode(patterns)
    }

    private const val WORD_BREAK = " "
}
