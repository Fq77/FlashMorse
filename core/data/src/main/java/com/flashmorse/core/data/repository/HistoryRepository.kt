package com.flashmorse.core.data.repository

import android.content.Context
import com.flashmorse.core.data.storage.JsonFileStore
import com.flashmorse.core.model.HistoryEntry
import com.flashmorse.core.model.TransmissionChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

/** 发送历史（F-10）：最近 50 条，支持一键重发。 */
class HistoryRepository(context: Context) {

    private val store = JsonFileStore(
        file = File(context.applicationContext.filesDir, "history.json"),
        serializer = HistoryFile.serializer(),
        defaultValue = { HistoryFile() },
    )

    private val mutex = Mutex()

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    suspend fun load() = mutex.withLock {
        val file = withContext(Dispatchers.IO) { store.read() }
        _entries.value = file.items.map { it.toEntry() }.take(MAX_ENTRIES)
    }

    suspend fun record(text: String, wpm: Int, channel: TransmissionChannel) = mutex.withLock {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withLock
        val entry = HistoryEntry(
            id = "h-${System.currentTimeMillis()}",
            text = trimmed,
            sentAtEpochMillis = System.currentTimeMillis(),
            wpm = wpm,
            channel = channel,
        )
        val next = (listOf(entry) + _entries.value).take(MAX_ENTRIES)
        _entries.value = next
        persist(next)
    }

    suspend fun delete(id: String) = mutex.withLock {
        val next = _entries.value.filterNot { it.id == id }
        _entries.value = next
        persist(next)
    }

    suspend fun clear() = mutex.withLock {
        _entries.value = emptyList()
        persist(emptyList())
    }

    private suspend fun persist(items: List<HistoryEntry>) = withContext(Dispatchers.IO) {
        store.write(HistoryFile(items = items.map { it.toRecord() }))
    }

    private fun HistoryEntry.toRecord() = HistoryRecord(id, text, sentAtEpochMillis, wpm, channel.name)

    private fun HistoryRecord.toEntry() = HistoryEntry(
        id = id,
        text = text,
        sentAtEpochMillis = sentAtEpochMillis,
        wpm = wpm,
        channel = TransmissionChannel.entries.firstOrNull { it.name == channel } ?: TransmissionChannel.Led,
    )

    private companion object {
        const val MAX_ENTRIES = 50
    }
}

@Serializable
internal data class HistoryFile(
    val version: Int = 1,
    val items: List<HistoryRecord> = emptyList(),
)

@Serializable
internal data class HistoryRecord(
    val id: String,
    val text: String,
    val sentAtEpochMillis: Long,
    val wpm: Int,
    val channel: String,
)
