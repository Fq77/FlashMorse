package com.flashmorse.core.torch

import com.flashmorse.core.model.TransmissionChannel

/**
 * 发光通道抽象（规划文档 §5.4）。
 *
 * 上层调度器只认「在 t 时刻把灯设为 on/off」，不关心底层是 LED 还是屏幕，
 * 因此通道切换对业务完全透明，也便于用 Mock 控制器做时序精度测试。
 */
interface TorchController {

    val channel: TransmissionChannel

    /** 通道是否可用（无闪光灯设备上 LED 通道为 false）。 */
    val isAvailable: Boolean

    /** 设置灯态。实现必须尽量少做工作，避免在时序线程上产生抖动。 */
    fun setOn(on: Boolean)

    /** 释放底层资源（注销回调等）。 */
    fun release()
}

/** 记录灯态翻转的控制台实现，供单元测试断言时序。 */
class RecordingTorchController(
    override val channel: TransmissionChannel = TransmissionChannel.Led,
    private val clock: () -> Long,
) : TorchController {

    data class Flip(val atNanos: Long, val on: Boolean)

    private val _flips = ArrayList<Flip>(256)
    val flips: List<Flip> get() = synchronized(_flips) { _flips.toList() }

    override val isAvailable: Boolean = true

    override fun setOn(on: Boolean) {
        synchronized(_flips) { _flips += Flip(clock(), on) }
    }

    override fun release() = Unit

    fun clear() {
        synchronized(_flips) { _flips.clear() }
    }
}
