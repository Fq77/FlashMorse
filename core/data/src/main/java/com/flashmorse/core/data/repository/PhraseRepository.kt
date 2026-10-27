package com.flashmorse.core.data.repository

import android.content.Context
import com.flashmorse.core.data.storage.JsonFileStore
import com.flashmorse.core.model.Phrase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

/**
 * 短语库（F-09）。
 *
 * 内置短语在首次启动时落盘，此后与自定义短语一视同仁——因此「内置短语可编辑、删除」
 * （验收 A-05）不需要额外分支，也不会在重启后重新长回来。
 */
class PhraseRepository(context: Context) {

    private val store = JsonFileStore(
        file = File(context.applicationContext.filesDir, "phrases.json"),
        serializer = PhraseFile.serializer(),
        defaultValue = { PhraseFile() },
    )

    private val mutex = Mutex()
    private var loaded = false

    private val _phrases = MutableStateFlow<List<Phrase>>(emptyList())
    val phrases: StateFlow<List<Phrase>> = _phrases.asStateFlow()

    suspend fun load() = mutex.withLock {
        if (loaded) return@withLock
        val file = withContext(Dispatchers.IO) { store.read() }
        val items = if (file.seeded || file.items.isNotEmpty()) {
            file.items
        } else {
            BuiltInPhrases.seed()
        }
        _phrases.value = items.map { it.toPhrase() }
        loaded = true
        persist(items)
    }

    suspend fun add(label: String, payload: String): Phrase = mutex.withLock {
        val record = PhraseRecord(
            // 同 HistoryRepository：id 是列表 key，毫秒时间戳会撞。
            id = "custom-${UUID.randomUUID()}",
            label = label.trim().ifBlank { payload.trim() },
            payload = payload.trim(),
            builtIn = false,
        )
        val next = _phrases.value.map { it.toRecord() } + record
        persist(next)
        _phrases.value = next.map { it.toPhrase() }
        record.toPhrase()
    }

    suspend fun update(id: String, label: String, payload: String) = mutex.withLock {
        val next = _phrases.value.map { phrase ->
            if (phrase.id == id) {
                phrase.copy(label = label.trim().ifBlank { payload.trim() }, payload = payload.trim())
            } else {
                phrase
            }
        }
        persist(next.map { it.toRecord() })
        _phrases.value = next
    }

    suspend fun delete(id: String) = mutex.withLock {
        val next = _phrases.value.filterNot { it.id == id }
        persist(next.map { it.toRecord() })
        _phrases.value = next
    }

    /** 把 [id] 移动到第 [toIndex] 位（拖拽排序），返回是否发生了移动。 */
    suspend fun moveTo(id: String, toIndex: Int): Boolean = mutex.withLock {
        val list = _phrases.value.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        if (from < 0) return@withLock false
        val to = toIndex.coerceIn(0, list.lastIndex)
        if (to == from) return@withLock false
        val item = list.removeAt(from)
        list.add(to, item)
        persist(list.map { it.toRecord() })
        _phrases.value = list
        true
    }

    private suspend fun persist(items: List<PhraseRecord>) = withContext(Dispatchers.IO) {
        store.write(PhraseFile(seeded = true, items = items))
    }

    private fun Phrase.toRecord() = PhraseRecord(id, label, payload, builtIn)

    private fun PhraseRecord.toPhrase() = Phrase(id = id, label = label, payload = payload, builtIn = builtIn)
}

@Serializable
internal data class PhraseFile(
    val version: Int = 1,
    val seeded: Boolean = false,
    val items: List<PhraseRecord> = emptyList(),
)

@Serializable
internal data class PhraseRecord(
    val id: String,
    val label: String,
    val payload: String,
    val builtIn: Boolean = false,
)

/**
 * 内置短语。
 *
 * label 用中文、payload 用英文/数字——v1.0 不做中文电报码（架构文档 §3.2），
 * 直接发中文会退化为 `?`，因此把中文留在展示层。
 */
internal object BuiltInPhrases {
    fun seed(): List<PhraseRecord> = listOf(
        PhraseRecord("builtin-sos", "求救", "SOS", builtIn = true),
        PhraseRecord("builtin-need-help", "需要帮助", "NEED HELP", builtIn = true),
        PhraseRecord("builtin-roger", "收到", "ROGER", builtIn = true),
        PhraseRecord("builtin-ok", "可以", "OK", builtIn = true),
        PhraseRecord("builtin-wait", "等待", "WAIT", builtIn = true),
        PhraseRecord("builtin-stop", "停止", "STOP", builtIn = true),
    )
}
