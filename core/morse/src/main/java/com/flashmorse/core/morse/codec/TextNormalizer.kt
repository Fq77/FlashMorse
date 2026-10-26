package com.flashmorse.core.morse.codec

/**
 * 报文归一化：大小写、全角转半角、中文标点映射、非法字符替换为 `?`。
 *
 * 目标不是「智能纠错」，而是保证任何输入都能被完整编码——宁可发出 `?`，
 * 也不能静默丢字（验收标准 A-02 要求无丢字、无乱序）。
 */
object TextNormalizer {

    private val cjkPunctuation: Map<Char, Char> = mapOf(
        '，' to ',',
        '。' to '.',
        '、' to ',',
        '？' to '?',
        '！' to '!',
        '：' to ':',
        '；' to ';',
        '（' to '(',
        '）' to ')',
        '“' to '"',
        '”' to '"',
        '‘' to '\'',
        '’' to '\'',
        '《' to '(',
        '》' to ')',
        '－' to '-',
        '～' to '-',
        '＋' to '+',
        '＝' to '=',
        '＆' to '&',
        '　' to ' ',
    )

    /** 归一化整段报文。 */
    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        val sb = StringBuilder(raw.length)
        var lastWasSpace = false
        for (ch in raw) {
            val mapped = mapChar(ch)
            if (mapped == ' ') {
                if (!lastWasSpace && sb.isNotEmpty()) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                sb.append(mapped)
                lastWasSpace = false
            }
        }
        return sb.toString().trim()
    }

    /** 单字符归一化：能编码则返回可编码字符，否则返回 `?`。 */
    fun mapChar(ch: Char): Char {
        if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') return ' '

        val normalized = when {
            ch.code in 0xFF01..0xFF5E -> (ch.code - 0xFEE0).toChar()
            cjkPunctuation.containsKey(ch) -> cjkPunctuation.getValue(ch)
            else -> ch
        }

        val upper = normalized.uppercaseChar()
        return if (MorseTable.supports(upper)) upper else '?'
    }

    /** 归一化并按单词切分（已去掉空串）。 */
    fun words(raw: String): List<String> =
        normalize(raw).split(' ').filter { it.isNotEmpty() }
}
