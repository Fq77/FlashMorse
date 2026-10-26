package com.flashmorse.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flashmorse.core.model.TimingProfile
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/**
 * 设置首页。
 *
 * 速度只在这里调节——点进子页用满宽度的滑杆，调速建议与可靠性说明也归位在子页里；
 * 「时序补偿 / 设备诊断 / 关于」同样是压栈子页面，不再用底部弹层。
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onRetainBrightnessChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onChannelChange: (Int) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenCalibration: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    ) {
        item { SmallTitle(text = "发报") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                ArrowPreference(
                    title = "速度",
                    summary = "当前 ${state.wpm} WPM · 点 ${state.unitMillis} ms · 划 ${state.unitMillis * 3} ms",
                    onClick = onOpenSpeed,
                )
                SwitchPreference(
                    title = "发报声音",
                    summary = "发报时同步播放滴声（600 Hz），灯亮即响、灯灭即停",
                    checked = state.soundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                )
                SwitchPreference(
                    title = "保留手电筒亮度",
                    summary = state.retainBrightnessSummary,
                    checked = state.retainTorchBrightness,
                    onCheckedChange = onRetainBrightnessChange,
                    enabled = state.brightnessAvailable,
                )
                OverlayDropdownPreference(
                    title = "发送通道",
                    summary = if (state.ledAvailable) {
                        "自动模式下 15 WPM 以上切到屏幕通道"
                    } else {
                        "未检测到闪光灯，固定使用屏幕闪光"
                    },
                    items = state.channelItems,
                    selectedIndex = state.channelIndex,
                    onSelectedIndexChange = onChannelChange,
                )
                SwitchPreference(
                    title = "循环发送",
                    summary = "长报文默认单次发送，打开后发送结束会自动重播",
                    checked = state.loop,
                    onCheckedChange = onLoopChange,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { SmallTitle(text = "反馈") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                SwitchPreference(
                    title = "震动反馈",
                    summary = "手动发报时按下给出触觉节拍",
                    checked = state.haptic,
                    onCheckedChange = onHapticChange,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { SmallTitle(text = "校准与诊断") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                ArrowPreference(
                    title = "时序补偿",
                    summary = "当前 ${formatCompensation(state.compensationMillis)} · 实测硬件延迟后整体前移时间轴",
                    onClick = onOpenCalibration,
                )
                ArrowPreference(
                    title = "设备诊断",
                    summary = buildString {
                        append(if (state.ledAvailable) "闪光灯可用" else "无闪光灯")
                        append(" · 热状态 ")
                        append(state.thermalLevel.displayName)
                    },
                    onClick = onOpenDiagnostics,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
        item { SmallTitle(text = "关于") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                ArrowPreference(
                    title = "关于闪光摩斯",
                    summary = "版本 ${state.appVersion} · 光信号发报机",
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

internal fun formatCompensation(millis: Int): String =
    if (millis == 0) "0 ms" else if (millis > 0) "+$millis ms" else "$millis ms"
