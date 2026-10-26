package com.flashmorse.core.data.prefs

import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.TimingProfile

/**
 * 用户设置。全部为本地键值，不联网、不上传（规划文档 §4.6）。
 */
data class Settings(
    val wpm: Int = TimingProfile.DEFAULT_WPM,
    val channel: ChannelPreference = ChannelPreference.Auto,
    /** 手动模式的震动节拍（规划文档 §4.5） */
    val hapticFeedback: Boolean = true,
    /** 发送结束后是否自动循环（SOS 以外的长报文默认不循环） */
    val loopByDefault: Boolean = false,
    /** 实机延迟补偿 L_cmd（毫秒），由校准流程写入 */
    val compensationMillis: Int = 0,
    /** 是否已确认过 SOS 免责提示（规划文档 §4.6） */
    val sosDisclaimerAccepted: Boolean = false,
    /** 上次编辑的报文，用于冷启动恢复 */
    val lastMessage: String = "SOS",
    /** 是否已看过「强光环境建议遮挡屏幕」提示 */
    val glareHintShown: Boolean = false,
    /** 发光亮度（百分比）：屏幕通道按灰度、LED 通道按硬件档位映射，见 [Brightness] */
    val brightnessPercent: Int = Brightness.DEFAULT_PERCENT,
) {
    val timingProfile: TimingProfile
        get() = TimingProfile.forWpm(wpm, compensationMillis)
}
