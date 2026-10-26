package com.flashmorse.feature.settings

import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.torch.ThermalLevel

/** 设置页的不可变 UI 状态。 */
data class SettingsUiState(
    val wpm: Int = 10,
    val unitMillis: Int = 120,
    val channel: ChannelPreference = ChannelPreference.Auto,
    val loop: Boolean = false,
    val haptic: Boolean = true,
    val compensationMillis: Int = 0,
    val ledAvailable: Boolean = true,
    val supportsStrength: Boolean = false,
    val cameraId: String? = null,
    val thermalLevel: ThermalLevel = ThermalLevel.Unknown,
    val appVersion: String = "1.0",
) {
    val channelItems: List<String> = ChannelPreference.entries.map { it.displayName }

    val channelIndex: Int get() = ChannelPreference.entries.indexOf(channel)

    val ledReliability: String
        get() = if (unitMillis >= 80) {
            "当前速度在 LED 通道的可靠范围内"
        } else {
            "当前速度超过 15 WPM，LED 通道存在时序失真风险，建议使用屏幕闪光"
        }
}
