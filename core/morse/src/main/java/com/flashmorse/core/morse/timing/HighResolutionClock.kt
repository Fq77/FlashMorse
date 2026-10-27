package com.flashmorse.core.morse.timing

/**
 * 高分辨率单调时钟抽象（架构文档 §5.3）。
 *
 * Android 侧实现必须基于 `SystemClock.elapsedRealtimeNanos()`，
 * 因为它包含深度休眠时间，而 `System.nanoTime()` 不包含。
 */
interface HighResolutionClock {
    fun nanos(): Long
    fun millis(): Long = nanos() / 1_000_000L
}

/** 纯 JVM 默认实现，供单元测试与预览使用。 */
object SystemHighResolutionClock : HighResolutionClock {
    override fun nanos(): Long = System.nanoTime()
}
