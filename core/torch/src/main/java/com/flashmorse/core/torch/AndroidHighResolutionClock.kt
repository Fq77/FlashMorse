package com.flashmorse.core.torch

import android.os.Build
import android.os.SystemClock
import com.flashmorse.core.morse.timing.HighResolutionClock

/**
 * Android 侧时钟实现：`elapsedRealtimeNanos` 包含深度休眠时间，
 * 不像 `System.nanoTime()` 会在息屏休眠时停摆（架构文档 §5.3）。
 */
object AndroidHighResolutionClock : HighResolutionClock {
    override fun nanos(): Long = SystemClock.elapsedRealtimeNanos()
}

internal val isApi33OrAbove: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
