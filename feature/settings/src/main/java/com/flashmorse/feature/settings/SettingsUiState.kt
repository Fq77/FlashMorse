package com.flashmorse.feature.settings

import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.torch.ThermalLevel

/** 设置页的不可变 UI 状态。 */
data class SettingsUiState(
    val wpm: Int = 10,
    val unitMillis: Int = 120,
    /** 持久化设置是否已首次加载：未加载前滑杆会以默认值渲染、再动画到保存值。 */
    val loaded: Boolean = false,
    val channel: ChannelPreference = ChannelPreference.Auto,
    val loop: Boolean = false,
    val haptic: Boolean = true,
    val compensationMillis: Int = 0,
    /** 手电筒亮度百分比（屏幕闪光通道恒定全亮，不参与调光） */
    val brightnessPercent: Int = Brightness.DEFAULT_PERCENT,
    /** 是否保留手电筒亮度（关闭后每次打开手电筒都回到 100%） */
    val retainTorchBrightness: Boolean = true,
    /** 发报时同步播放滴声 */
    val soundEnabled: Boolean = false,
    val ledAvailable: Boolean = true,
    val supportsStrength: Boolean = false,
    /** 硬件档位上限，用于把百分比换算成"第几档"给用户看 */
    val maxStrengthLevel: Int = 0,
    val cameraId: String? = null,
    val thermalLevel: ThermalLevel = ThermalLevel.Unknown,
    val appVersion: String = "1.0",
) {
    val channelItems: List<String> = ChannelPreference.entries.map { it.displayName }

    val channelIndex: Int get() = ChannelPreference.entries.indexOf(channel)

    /** 亮度项是否可用：没有闪光灯或硬件不分档时，这个设置没有意义。 */
    val brightnessAvailable: Boolean
        get() = ledAvailable && supportsStrength

    /** 亮度开关的说明文案：只讲手电筒，别让人以为它也会影响屏幕闪光。 */
    val retainBrightnessSummary: String
        get() = when {
            !ledAvailable -> "本机没有闪光灯"
            !supportsStrength -> "本机闪光灯不支持档位调节"
            retainTorchBrightness -> "手电筒记住上次调光的亮度"
            else -> "每次打开手电筒都回到 100%"
        }

    val ledReliability: String
        get() = if (unitMillis >= 80) {
            "当前速度在 LED 通道的可靠范围内"
        } else {
            "当前速度超过 15 WPM，LED 通道存在时序失真风险，建议使用屏幕闪光"
        }
}
