package com.flashmorse.core.data.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 极简 JSON 文件存储：原子写入 + 容忍损坏。
 *
 * 短语库（数十条）与历史记录（≤ 50 条）的数据量远未到需要关系型存储的规模，
 * 因此 v1.0 用 JSON 文件替代 Room，把构建链路上的注解处理器依赖降到零；
 * Repository 接口保持与 Room 版本一致，后续可局部替换。
 */
internal class JsonFileStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    private val defaultValue: () -> T,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun read(): T = runCatching {
        if (!file.exists()) return@runCatching defaultValue()
        val text = file.readText()
        if (text.isBlank()) return@runCatching defaultValue()
        json.decodeFromString(serializer, text)
    }.getOrElse { defaultValue() }

    fun write(value: T) {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(json.encodeToString(serializer, value))
            if (file.exists()) file.delete()
            temp.renameTo(file)
        }
    }
}
