package com.flashmorse.feature.manual

import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.model.TransmissionChannel

/** 手动发报页的不可变 UI 状态。 */
data class ManualUiState(
    val wpm: Int = TimingProfile.DEFAULT_WPM,
    val unitMillis: Int = TimingProfile.unitMillisForWpm(TimingProfile.DEFAULT_WPM),
    val channel: TransmissionChannel = TransmissionChannel.Led,
    val pressed: Boolean = false,
    val pressedMillis: Long = 0L,
    /** 当前正在组字的点划模式，例如 `.-` */
    val pattern: String = "",
    /** 已解码文本（可能带尾随空格，展示时 trim）。 */
    val text: String = "",
    val lastChar: Char? = null,
    val hapticFeedback: Boolean = true,
) {
    /**
     * 点 / 划判定阈值。
     *
     * 规划文档 A-03 写作「按住 < 0.5 单位时长判定为点」，与 §11.1 的「点 1t、划 3t」
     * 相互矛盾（0.5t 比一个点本身还短，几乎不可能由人手触发）。此处取与 ITU 一致的解释：
     * 阈值落在点（1t）与划（3t）的中点，即 2t。
     */
    val dashThresholdMillis: Int get() = unitMillis * 2

    val displayText: String get() = text.trimEnd()

    val isEmpty: Boolean get() = displayText.isEmpty()
}
