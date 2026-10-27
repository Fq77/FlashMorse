package com.flashmorse.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashmorse.core.data.prefs.Settings
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.model.isActive
import com.flashmorse.core.morse.codec.MorseEncoder
import com.flashmorse.core.morse.timing.ScriptCompiler
import com.flashmorse.core.torch.MorseTransmitter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 时序补偿子页面：整页而不是弹层，滑杆可以用满宽度。 */
@Composable
fun CalibrationRoute(
    settingsRepository: SettingsRepository,
    transmitter: MorseTransmitter,
    modifier: Modifier = Modifier,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val playback by transmitter.state.collectAsStateWithLifecycle()
    var value by remember { mutableIntStateOf(settings.compensationMillis) }
    val scope = rememberCoroutineScope()
    val previewing = playback.isActive

    LaunchedEffect(settings.compensationMillis) {
        if (settings.compensationMillis != value) value = settings.compensationMillis
    }

    CalibrationScreen(
        compensationMillis = value,
        wpm = settings.wpm,
        brightnessPercent = settings.effectiveBrightnessPercent,
        previewing = previewing,
        onValueChange = { value = (it / 5f).roundToInt() * 5 },
        onValueCommitted = {
            scope.launch { settingsRepository.setCompensation(value) }
        },
        onPreview = {
            // 同一个按钮兼作停止：试闪时点它立刻灭灯，不用等这一遍 SOS 走完。
            if (previewing) {
                transmitter.stop()
            } else {
                val timing = TimingProfile.forWpm(settings.wpm, value)
                val script = ScriptCompiler.compile(MorseEncoder().encode("SOS"), timing)
                val channel = transmitter.resolveChannel(timing)
                transmitter.play(script, loop = false, channel = channel)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun CalibrationScreen(
    compensationMillis: Int,
    wpm: Int,
    brightnessPercent: Int,
    previewing: Boolean,
    onValueChange: (Float) -> Unit,
    onValueCommitted: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val unitMillis = TimingProfile.unitMillisForWpm(wpm)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        SmallTitle(text = "补偿值")

        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "指令提前量",
                        style = MiuixTheme.textStyles.subtitle,
                        color = colorScheme.onBackground,
                    )
                    Text(
                        text = formatCompensation(compensationMillis),
                        style = MiuixTheme.textStyles.title3,
                        color = colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = compensationMillis.toFloat(),
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueCommitted,
                    valueRange = -60f..60f,
                    steps = 23,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "−60 ms",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = "+60 ms",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "为什么要补偿",
                    style = MiuixTheme.textStyles.subtitle,
                    color = colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "闪光灯从「收到指令」到「真正亮起」有 30–300 ms 不等的硬件延迟，" +
                        "机型差异极大。补偿值会把整条时间轴整体前移，让光出现的时刻与预期对齐；" +
                        "因为是整体平移，点划比例和总时长都不受影响。",
                    style = MiuixTheme.textStyles.footnote1,
                    color = colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "当前速度 $wpm WPM · 单位时长 ${unitMillis} ms。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append("手电筒通道按当前亮度 $brightnessPercent% 发出；" +
                            "屏幕闪光通道始终整屏全亮，不受此设定影响。")
                        if (brightnessPercent < Brightness.LOW_THRESHOLD_PERCENT) {
                            append("手电筒低亮度下点划边沿更软，此刻标定的补偿值偏保守，")
                            append("建议调到 60% 以上再校准。")
                        }
                    },
                    style = MiuixTheme.textStyles.footnote2,
                    color = if (brightnessPercent < Brightness.LOW_THRESHOLD_PERCENT) {
                        colorScheme.primary
                    } else {
                        colorScheme.onSurfaceVariantSummary
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onPreview,
            colors = if (previewing) {
                ButtonDefaults.buttonColors(
                    color = colorScheme.errorContainer,
                    contentColor = colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.buttonColorsPrimary()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = if (previewing) "停止试闪" else "试闪 SOS")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
