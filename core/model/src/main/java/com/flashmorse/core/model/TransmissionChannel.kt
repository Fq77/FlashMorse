package com.flashmorse.core.model

/** 承载光信号的物理通道（规划文档 §11.3）。 */
enum class TransmissionChannel {
    /** LED 手电筒：远距离、抗环境光，延迟 30–300 ms */
    Led,

    /** 屏幕白闪：低延迟（受帧率约束）、100% 可用 */
    Screen,
    ;

    val displayName: String
        get() = when (this) {
            Led -> "手电筒"
            Screen -> "屏幕闪光"
        }
}

/** 用户对通道的偏好。 */
enum class ChannelPreference {
    Auto,
    Led,
    Screen,
    ;

    val displayName: String
        get() = when (this) {
            Auto -> "自动"
            Led -> "手电筒"
            Screen -> "屏幕闪光"
        }
}
