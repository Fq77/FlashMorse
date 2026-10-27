package com.flashmorse.core.model

/**
 * 摩斯时序的基本元素（对齐架构文档 §5.3）。
 *
 * 一个字符由若干 [Dot] / [Dash] 组成，元素之间是 [ElementGap]；
 * 字符之间是 [CharGap]，单词之间是 [WordGap]。
 */
sealed interface MorseSymbol {

    /** 点（·），时长 1t */
    data object Dot : MorseSymbol

    /** 划（—），时长 3t */
    data object Dash : MorseSymbol

    /** 同一字符内的元素间隔，时长 1t */
    data object ElementGap : MorseSymbol

    /** 字符间隔，时长 3t */
    data object CharGap : MorseSymbol

    /** 单词间隔，时长 7t */
    data object WordGap : MorseSymbol

    companion object {
        /** 由码表字符（`.` / `-`）构造信号元素。 */
        fun ofSignal(char: Char): MorseSymbol? = when (char) {
            '.' -> Dot
            '-' -> Dash
            else -> null
        }
    }
}

/** 该元素是否需要点亮光源。 */
val MorseSymbol.isSignal: Boolean
    get() = when (this) {
        MorseSymbol.Dot, MorseSymbol.Dash -> true
        MorseSymbol.ElementGap, MorseSymbol.CharGap, MorseSymbol.WordGap -> false
    }

/** 用于符号流渲染的短标签，例如点显示为 `·`。 */
val MorseSymbol.glyph: String
    get() = when (this) {
        MorseSymbol.Dot -> "·"
        MorseSymbol.Dash -> "—"
        MorseSymbol.ElementGap -> ""
        MorseSymbol.CharGap -> " "
        MorseSymbol.WordGap -> "   "
    }
