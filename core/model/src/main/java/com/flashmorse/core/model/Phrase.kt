package com.flashmorse.core.model

/**
 * 短语库条目。
 *
 * [label] 是列表展示文案，[payload] 是实际发送的报文——
 * 二者分离使得中文标签可以驱动英文/数字报文（v1.0 不做中文电报码，见规划文档 §3.2）。
 */
data class Phrase(
    val id: String,
    val label: String,
    val payload: String,
    val builtIn: Boolean = false,
) {
    val isCustom: Boolean get() = !builtIn
}

/** 历史记录条目（最近 50 条）。 */
data class HistoryEntry(
    val id: String,
    val text: String,
    val sentAtEpochMillis: Long,
    val wpm: Int,
    val channel: TransmissionChannel,
)
