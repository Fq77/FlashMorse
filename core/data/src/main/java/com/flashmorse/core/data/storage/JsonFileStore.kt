package com.flashmorse.core.data.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

    /**
     * 原子写入，返回是否成功。
     *
     * 先写 `.tmp` 再整体替换：写入期间崩溃只会留下一个未完成的临时文件，
     * 原文件仍是上一份完整数据。此前「delete 旧文件 → rename 临时文件」的做法
     * 在这两步之间崩溃会直接丢掉全部短语与历史。
     */
    fun write(value: T): Boolean = runCatching {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(json.encodeToString(serializer, value))
        try {
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            // 少数文件系统（部分外置存储 / FUSE 层）不支持原子移动，退化为常规替换
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        true
    }.getOrDefault(false)
}
