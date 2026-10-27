package com.flashmorse.core.torch

import com.flashmorse.core.model.TransmissionChannel

/** 设备发光能力描述（架构文档 §5.4）。 */
data class TorchCapabilities(
    /** 后置摄像头 ID；无闪光灯时为 null。 */
    val cameraId: String? = null,
    val hasFlash: Boolean = false,
    /** 是否支持 `turnOnTorchWithStrengthLevel`（API 33+ 且硬件支持）。 */
    val supportsStrength: Boolean = false,
    /** 硬件上报的最高档位；1 表示只有开/关两态。 */
    val maxStrengthLevel: Int = 0,
    /** 硬件上报的默认档位（`setTorchMode` 点亮时使用的档位），仅用于诊断展示。 */
    val defaultStrengthLevel: Int = 1,
) {

    /**
     * 可调范围的下界。
     *
     * 必须是 1（最暗档），不能取 [defaultStrengthLevel]——默认档不是最小档，
     * 若机型默认档恰好等于最高档，滑杆范围会被压成 0，表现就是"怎么滑都不变"。
     */
    val minStrengthLevel: Int get() = if (supportsStrength) 1 else 0
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
