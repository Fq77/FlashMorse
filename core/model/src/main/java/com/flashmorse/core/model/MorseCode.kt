package com.flashmorse.core.model

/**
 * 一个摩斯字符。[pattern] 仅由 `.` 与 `-` 组成，例如 `A` -> `.-`。
 */
data class MorseLetter(
    val char: Char,
    val pattern: String,
) {
    init {
        require(pattern.isNotEmpty()) { "pattern must not be empty" }
        require(pattern.all { it == '.' || it == '-' }) { "pattern must contain only '.' and '-'" }
    }

    /** 按顺序展开的信号元素。 */
    val symbols: List<MorseSymbol> =
        pattern.map { MorseSymbol.ofSignal(it) ?: MorseSymbol.Dot }

    /** 人类可读形式，点用 `·`、划用 `—`。 */
    val display: String = pattern.map { if (it == '.') '·' else '—' }.joinToString("")
}

/** 一个单词由若干字母组成。 */
data class MorseWord(val letters: List<MorseLetter>) {
    init {
        require(letters.isNotEmpty()) { "word must not be empty" }
    }
}

/**
 * 一段完整的摩斯报文，按单词分组。
 *
 * 编码器输出该结构，[com.flashmorse.core.morse.timing.ScriptCompiler] 再把它编译成
 * 绝对时间轴 [SendScript]。
 */
data class MorseCode(val words: List<MorseWord>) {

    /** 展平后的字母序列，顺序与发送顺序一致。 */
    val letters: List<MorseLetter> = words.flatMap { it.letters }

    val isEmpty: Boolean get() = letters.isEmpty()

    val letterCount: Int get() = letters.size

    /** 还原成可读文本（单词以单空格分隔）。 */
    val plainText: String
        get() = words.joinToString(" ") { word -> word.letters.joinToString("") { it.char.toString() } }

    companion object {
        val Empty = MorseCode(emptyList())
    }
}
