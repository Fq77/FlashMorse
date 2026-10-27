package com.flashmorse.core.morse.codec

import com.flashmorse.core.model.MorseSymbol

/**
 * 符号/模式 → 文本，供手动发报模式的回显使用（架构文档 §5.3）。
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

    /**
     * 从元素序列解码（间隔元素触发断字/断词）。
     *
     * 关键区分：[MorseSymbol.ElementGap] 是**字符内部**的 1t 间隔，
     * 不能切分当前 pattern——否则 `....` 会被拆成四个 `E`。
     * 只有 [MorseSymbol.CharGap]（3t）才结束一个字符。
     */
    fun decodeSymbols(symbols: List<MorseSymbol>): String {
        val patterns = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                patterns += current.toString()
                current.clear()
            }
        }

        for (symbol in symbols) {
            when (symbol) {
                MorseSymbol.Dot -> current.append('.')
                MorseSymbol.Dash -> current.append('-')
                MorseSymbol.ElementGap -> Unit
                MorseSymbol.CharGap -> flush()
                MorseSymbol.WordGap -> {
                    flush()
                    patterns += WORD_BREAK
                }
            }
        }
        flush()
        return decode(patterns)
    }

    private const val WORD_BREAK = " "
}
