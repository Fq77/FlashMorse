package com.flashmorse.feature.library

import com.flashmorse.core.model.HistoryEntry
import com.flashmorse.core.model.Phrase

enum class LibraryTab { Phrases, History }

/** 短语列表行需要的展示数据：条目 + 首字符点划预览。 */
data class PhraseItem(
    val phrase: Phrase,
    val previewPattern: String,
)

/** 短语编辑对话框状态。 */
data class PhraseEditor(
    val id: String? = null,
    val label: String = "",
    val payload: String = "",
) {
    val isNew: Boolean get() = id == null
    val canSave: Boolean get() = payload.isNotBlank()
}

/** 短语库页的不可变 UI 状态。 */
data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.Phrases,
    val phrases: List<PhraseItem> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val editor: PhraseEditor? = null,
    val menuTarget: Phrase? = null,
    val playingText: String? = null,
    val isPlaying: Boolean = false,
    val wpm: Int = 10,
)
