package com.flashmorse.core.morse.prosign

import com.flashmorse.core.model.MorseCode
import com.flashmorse.core.model.MorseLetter
import com.flashmorse.core.model.MorseWord

/**
 * 过程信号（prosign）：由连续符号组成、不按字符间隔拆分的约定信号（规划文档 §11.2）。
 */
data class Prosign(
    val name: String,
    val pattern: String,
    val meaning: String,
)

object Prosigns {

    /** 国际求救信号 `···———···`，作为单个字符连续发送。 */
    val Sos = Prosign("SOS", "...---...", "国际求救信号")

    val Sk = Prosign("SK", "...-.-", "结束联络")
    val Ar = Prosign("AR", ".-.-.", "消息结束")
    val K = Prosign("K", "-.-", "请发送")
    val Bt = Prosign("BT", "-...-", "分隔符")
    val Ve = Prosign("VE", "...-.", "收到")
    val Hh = Prosign("HH", "........", "错误更正")

    val all: List<Prosign> = listOf(Sos, Sk, Ar, K, Bt, Ve, Hh)

    /**
     * 把 prosign 转为单字符的 [MorseCode]。
     *
     * 使用 `§` 作为占位字符——prosign 没有对应的可打印字符，
     * 展示层一律使用 [Prosign.name]，不要依赖该占位符。
     */
    fun toMorseCode(prosign: Prosign): MorseCode =
        MorseCode(listOf(MorseWord(listOf(MorseLetter(PLACEHOLDER, prosign.pattern)))))

    private const val PLACEHOLDER = '§'
}
