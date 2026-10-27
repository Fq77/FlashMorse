package com.flashmorse.core.torch

import com.flashmorse.core.model.TransmissionChannel

/**
 * 发光通道抽象（架构文档 §5.4）。
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

    /**
     * 是否支持软件调光。
     *
     * 默认 false：屏幕通道与 Mock 之外，只有硬件真的分档（API 33+ 且 HAL 报出多档）的 LED 才为 true。
     * UI 依据它决定是否展示/启用亮度项，而不是"滑了没反应"。
     */
    val supportsBrightness: Boolean get() = false

    /**
     * 设置亮度比例（0..1）。
     *
     * 只允许在空闲时调用：发送过程中改档会让同一段报文的点划边沿特性突变，
     * 而这正是接收端最难判读的变化。
     */
    fun setBrightness(fraction: Float) = Unit

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
