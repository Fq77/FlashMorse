package com.flashmorse.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashmorse.core.data.prefs.Settings
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.torch.MorseTransmitter
import com.flashmorse.core.torch.ThermalGuard
import com.flashmorse.core.torch.ThermalLevel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun DiagnosticsRoute(
    settingsRepository: SettingsRepository,
    transmitter: MorseTransmitter,
    thermalGuard: ThermalGuard,
    modifier: Modifier = Modifier,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val thermal by thermalGuard.level.collectAsStateWithLifecycle()
    val capabilities = remember { transmitter.capabilities() }

    DiagnosticsScreen(
        preference = settings.channel,
        ledAvailable = transmitter.ledAvailable,
        cameraId = capabilities?.cameraId,
        supportsStrength = transmitter.ledSupportsBrightness,
        maxStrengthLevel = capabilities?.maxStrengthLevel ?: 0,
        brightnessPercent = settings.effectiveBrightnessPercent,
        thermalLevel = thermal,
        wpm = settings.wpm,
        unitMillis = settings.timingProfile.unitMillis,
        modifier = modifier,
    )
}

@Composable
fun DiagnosticsScreen(
    preference: ChannelPreference,
    ledAvailable: Boolean,
    cameraId: String?,
    supportsStrength: Boolean,
    maxStrengthLevel: Int,
    brightnessPercent: Int,
    thermalLevel: ThermalLevel,
    wpm: Int,
    unitMillis: Int,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme

    // 建议必须针对"实际会用的通道"：通道选屏幕闪光时还去讲 LED 边沿，会让人以为设置没生效。
    val ledInUse = when (preference) {
        ChannelPreference.Led -> ledAvailable
        ChannelPreference.Screen -> false
        ChannelPreference.Auto -> ledAvailable && unitMillis >= 80
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        SmallTitle(text = "发光通道")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                DiagnosticRow("闪光灯", if (ledAvailable) "可用" else "不可用，已固定屏幕闪光")
                DiagnosticRow("后置相机 ID", cameraId ?: "—")
                DiagnosticRow(
                    "亮度分级",
                    if (supportsStrength) "支持（1–$maxStrengthLevel 档）" else "不支持",
                )
                DiagnosticRow(
                    "手电筒亮度",
                    if (supportsStrength) {
                        val level = Brightness.levelFor(
                            Brightness.percentToFraction(brightnessPercent),
                            maxStrengthLevel,
                        )
                        "$brightnessPercent% · 硬件 $level/$maxStrengthLevel 档"
                    } else {
                        "$brightnessPercent%（仅手电筒通道）"
                    },
                )
                DiagnosticRow("通道偏好", preference.displayName)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SmallTitle(text = "运行状态")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                DiagnosticRow("热状态", thermalLevel.displayName)
                DiagnosticRow("当前速度", "$wpm WPM · 单位 $unitMillis ms")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "建议",
                    style = MiuixTheme.textStyles.subtitle,
                    color = colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        !ledAvailable -> "本机无闪光灯，屏幕闪光通道是唯一选择，近距离使用效果最好。"
                        thermalLevel.requiresThrottle ->
                            "设备温度偏高，建议降低速度或间歇发送，避免热保护中断。"
                        brightnessPercent < Brightness.LOW_THRESHOLD_PERCENT && ledInUse ->
                            "当前亮度 $brightnessPercent%，LED 边沿变软：适合近距离示意，" +
                                "远距离请把亮度提到 60% 以上。"
                        unitMillis < 80 ->
                            "当前速度超过 15 WPM，LED 的 HAL 延迟会造成时序失真，建议切到屏幕闪光通道。"
                        else -> "当前配置在 LED 通道的可靠范围内，可以直接使用。"
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.width(112.dp),
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
    }
}
