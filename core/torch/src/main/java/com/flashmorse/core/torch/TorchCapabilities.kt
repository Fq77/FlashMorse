package com.flashmorse.core.torch

import com.flashmorse.core.model.TransmissionChannel

/** 设备发光能力描述（规划文档 §5.4）。 */
data class TorchCapabilities(
    /** 后置摄像头 ID；无闪光灯时为 null。 */
    val cameraId: String? = null,
    val hasFlash: Boolean = false,
    /** 是否支持 `turnOnTorchWithStrengthLevel`（API 33+ 且硬件支持）。 */
    val supportsStrength: Boolean = false,
    val maxStrengthLevel: Int = 0,
    val minStrengthLevel: Int = 0,
) {
    /** 可用通道，按优先级排列。 */
    val availableChannels: List<TransmissionChannel> = buildList {
        if (hasFlash) add(TransmissionChannel.Led)
        add(TransmissionChannel.Screen)
    }

    val ledAvailable: Boolean get() = hasFlash

    /** 面向用户的诊断文案。 */
    val summary: String
        get() = when {
            !hasFlash -> "未检测到可用闪光灯，将使用屏幕闪光通道"
            supportsStrength -> "闪光灯可用（支持亮度调节）"
            else -> "闪光灯可用"
        }
}
