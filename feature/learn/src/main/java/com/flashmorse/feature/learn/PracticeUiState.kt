package com.flashmorse.feature.learn

import com.flashmorse.core.model.MorseLetter

/**
 * 识读练习页状态。
 *
 * 一轮固定 [DEFAULT_ROUND_SIZE] 题；每题给出四个候选字符，用户从灯光（或侧音）
 * 里读出是哪一个。答错不隐藏正确答案——练习的目的是建立映射，不是考试。
 */
data class PracticeUiState(
    val question: MorseLetter? = null,
    val options: List<MorseLetter> = emptyList(),
    /** 用户选择的字符；未作答为 null。 */
    val selected: Char? = null,
    val answered: Boolean = false,
    val correctCount: Int = 0,
    /** 已作答题数。 */
    val askedCount: Int = 0,
    val roundSize: Int = DEFAULT_ROUND_SIZE,
    val finished: Boolean = false,
    /** 当前是否正在发光（由发送器状态回流，不本地维护）。 */
    val playing: Boolean = false,
    val wpm: Int = 10,
    val soundEnabled: Boolean = false,
) {
    val isCorrect: Boolean get() = answered && selected != null && selected == question?.char

    /** 当前题号；已答完最后一题时停在 roundSize。 */
    val questionNumber: Int get() = (askedCount + if (answered) 0 else 1).coerceIn(1, roundSize)

    val accuracyPercent: Int get() = if (askedCount == 0) 0 else correctCount * 100 / askedCount

    companion object {
        const val DEFAULT_ROUND_SIZE = 10
    }
}
