package com.flashmorse.feature.learn

import com.flashmorse.core.model.MorseLetter

enum class LearnTab { Letters, Digits, Punctuation }

/** 对照表页的不可变 UI 状态。 */
data class LearnUiState(
    val tab: LearnTab = LearnTab.Letters,
    val alphabet: List<MorseLetter> = emptyList(),
    val digits: List<MorseLetter> = emptyList(),
    val punctuation: List<MorseLetter> = emptyList(),
    val selected: MorseLetter? = null,
    val playingChar: Char? = null,
    val wpm: Int = 10,
) {
    val visible: List<MorseLetter>
        get() = when (tab) {
            LearnTab.Letters -> alphabet
            LearnTab.Digits -> digits
            LearnTab.Punctuation -> punctuation
        }
}
