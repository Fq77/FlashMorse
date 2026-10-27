package com.flashmorse.core.data.prefs

import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.TimingProfile

/**
 * 用户设置。全部为本地键值，不联网、不上传（架构文档 §4.6）。
 */
data class Settings(
    val wpm: Int = TimingProfile.DEFAULT_WPM,
    val channel: ChannelPreference = ChannelPreference.Auto,
    /** 手动模式的震动节拍（架构文档 §4.5） */
    val hapticFeedback: Boolean = true,
    /** 发送结束后是否自动循环（SOS 以外的长报文默认不循环） */
    val loopByDefault: Boolean = false,
    /** 实机延迟补偿 L_cmd（毫秒），由校准流程写入 */
    val compensationMillis: Int = 0,
    /** 上次编辑的报文，用于冷启动恢复；默认为空，避免一打开就处于可发送求救信号的状态 */
    val lastMessage: String = "",
    /** 手电筒亮度（百分比），仅 LED 通道使用；见 [Brightness] */
    val brightnessPercent: Int = Brightness.DEFAULT_PERCENT,
    /** 是否保留手电筒亮度：关闭后每次打开手电筒都回到 100%（已调的值仍被记住，重新开启即恢复） */
    val retainTorchBrightness: Boolean = true,
    /** 发报时是否同步播放滴声（600 Hz 侧音，灯亮即响） */
    val soundEnabled: Boolean = false,
) {
    val timingProfile: TimingProfile
        get() = TimingProfile.forWpm(wpm, compensationMillis)

    /** 实际下发给手电筒的亮度：不保留时恒为默认值，已调的值仍留在 [brightnessPercent] */
    val effectiveBrightnessPercent: Int
        get() = if (retainTorchBrightness) brightnessPercent else Brightness.DEFAULT_PERCENT
}
