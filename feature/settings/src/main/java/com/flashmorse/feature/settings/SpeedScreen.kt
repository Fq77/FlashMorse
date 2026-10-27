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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashmorse.core.data.prefs.Settings
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.model.TimingProfile
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 速度子页。
 *
 * 滑杆用满宽度、精确到 1 WPM；调速建议与「当前速度在 LED 通道的可靠性」
 * 从设置页底部归位到这里——它们回答的都是「这个速度意味着什么、怎么调」。
 */
@Composable
fun SpeedRoute(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier,
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val scope = rememberCoroutineScope()

    SpeedScreen(
        wpm = settings.wpm,
        onWpmChange = { wpm ->
            scope.launch { settingsRepository.setWpm(wpm) }
        },
        modifier = modifier,
    )
}

@Composable
fun SpeedScreen(
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val timing = TimingProfile.forWpm(wpm)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        SmallTitle(text = "发报速度")

        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "速度",
                        style = MiuixTheme.textStyles.subtitle,
                        color = colorScheme.onBackground,
                    )
                    Text(
                        text = "$wpm WPM",
                        style = MiuixTheme.textStyles.title3,
                        color = colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = wpm.toFloat(),
                    onValueChange = { onWpmChange(it.roundToInt()) },
                    valueRange = TimingProfile.MIN_WPM.toFloat()..TimingProfile.MAX_WPM.toFloat(),
                    steps = (TimingProfile.MAX_WPM - TimingProfile.MIN_WPM) - 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${TimingProfile.MIN_WPM} WPM",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = "${TimingProfile.MAX_WPM} WPM",
                        style = MiuixTheme.textStyles.footnote2,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SmallTitle(text = "这个速度意味着什么")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "点 ${timing.unitMillis} ms · 划 ${timing.dashMillis} ms · " +
                        "字符间隔 ${timing.charGapMillis} ms · 单词间隔 ${timing.wordGapMillis} ms",
                    style = MiuixTheme.textStyles.footnote1,
                    color = colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (timing.isLedReliable) {
                        "当前速度在 LED 通道的可靠范围内，闪光灯与屏幕闪光都可以直接使用。"
                    } else {
                        "当前速度超过 15 WPM，LED 通道存在时序失真风险，发报会自动使用屏幕闪光。"
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (timing.isLedReliable) {
                        colorScheme.onSurfaceVariantSummary
                    } else {
                        colorScheme.primary
                    },
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "调速后建议先发一段熟悉的报文核对节奏；若整体偏快或偏慢，" +
                        "再到「时序补偿」微调（每次 5 ms）。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
