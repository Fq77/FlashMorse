package com.flashmorse.core.torch.impl

import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.torch.TorchController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 屏幕全屏白闪通道（规划文档 §6.1 方案 D）。
 *
 * 低延迟（受帧率约束，通常 ≤ 16 ms）、100% 可用，作为 LED 不可用时的兜底，
 * 同时也是高速发报的推荐通道。
 *
 * 控制器本身不持有任何 UI：它只暴露 [lit] 状态，由 Compose 层根据该状态
 * 绘制全屏白色覆盖层，从而让调度器对「LED 还是屏幕」完全无感。
 */
class ScreenFlashController : TorchController {

    override val channel: TransmissionChannel = TransmissionChannel.Screen

    override val isAvailable: Boolean = true

    private val _lit = MutableStateFlow(false)

    /** 当前是否应点亮（true = 全屏白）。 */
    val lit: StateFlow<Boolean> = _lit.asStateFlow()

    // 注意：屏幕通道不实现调光（supportsBrightness 维持默认 false）。
    // 它的价值就是"最亮、最容易被看见"，调暗只会削弱唯一优势；
    // 亮度设定只作用于手电筒，见 TorchManagerController。

    override fun setOn(on: Boolean) {
        if (_lit.value != on) _lit.value = on
    }

    override fun release() {
        _lit.value = false
    }
}
