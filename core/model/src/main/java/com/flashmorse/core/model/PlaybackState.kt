package com.flashmorse.core.model

/** 播放中止/结束的原因（架构文档 §6.4）。 */
enum class AbortReason {
    /** 用户主动停止 */
    UserStopped,

    /** 热保护触发 */
    ThermalProtection,

    /** 手电筒被其他应用抢占 */
    ExternalTakeover,

    /** 通道不可用（无闪光灯且屏幕通道未就绪） */
    TorchUnavailable,

    /** 预期外错误 */
    Error,
    ;
}

/**
 * 播放器状态。必须穷尽且互斥，避免「正在发报又显示暂停」这类非法态。
 */
sealed interface PlaybackState {

    data object Idle : PlaybackState

    data class Preparing(val script: SendScript) : PlaybackState

    data class Transmitting(
        val script: SendScript,
        val currentIndex: Int,
        val currentSymbol: MorseSymbol,
        val elapsedMs: Long,
        val estimatedRemainingMs: Long,
        val loop: Boolean = false,
        /**
         * 该采样对应的单调时钟时刻（纳秒，`SystemClock.elapsedRealtimeNanos` 时基）。
         *
         * 进度条不能只在事件边界跳变：一个 60–240 ms 的符号单元里，UI 需要按帧外推，
         * 而外推必须知道「[elapsedMs] 是在哪一刻测到的」，否则会被主线程的合成延迟带偏。
         */
        val emittedAtNanos: Long = 0L,
    ) : PlaybackState

    data class Paused(
        val script: SendScript,
        val resumeIndex: Int,
    ) : PlaybackState

    data class Completed(val script: SendScript) : PlaybackState

    data class Aborted(val reason: AbortReason) : PlaybackState

    companion object {
        val initial: PlaybackState = Idle
    }
}

/** 是否处于「有脚本正在推进」的状态（含暂停）。 */
val PlaybackState.isActive: Boolean
    get() = when (this) {
        is PlaybackState.Transmitting, is PlaybackState.Paused, is PlaybackState.Preparing -> true
        else -> false
    }

/** 正在发送时返回脚本，否则返回 null。 */
val PlaybackState.activeScript: SendScript?
    get() = when (this) {
        is PlaybackState.Preparing -> script
        is PlaybackState.Transmitting -> script
        is PlaybackState.Paused -> script
        is PlaybackState.Completed -> script
        else -> null
    }
