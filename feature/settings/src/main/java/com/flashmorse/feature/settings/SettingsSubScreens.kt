package com.flashmorse.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashmorse.core.data.prefs.Settings
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.designsystem.icon.FmIcons
import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.model.isActive
import com.flashmorse.core.morse.codec.MorseEncoder
import com.flashmorse.core.morse.timing.ScriptCompiler
import com.flashmorse.core.torch.MorseTransmitter
import com.flashmorse.core.torch.ThermalGuard
import com.flashmorse.core.torch.ThermalLevel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ------------------------------------------------------------------ 时序补偿

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
        brightnessPercent = settings.brightnessPercent,
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
                val channel = transmitter.resolveChannel(ChannelPreference.Auto, timing)
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
                    text = "当前速度 $wpm WPM · 点 ${unitMillis} ms。建议先用默认值发一段熟悉的报文，" +
                        "若整体「慢了」再逐档增加，每次 5 ms。",
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

// ------------------------------------------------------------------ 设备诊断

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
        brightnessPercent = settings.brightnessPercent,
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
                        "$brightnessPercent%（仅屏幕通道）"
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

// ---------------------------------------------------------------------- 关于

/** Miuix 开源项目地址：致谢栏与「设计与许可」共用同一常量。 */
private const val MIUIX_REPO = "https://github.com/compose-miuix-ui/miuix"

/**
 * 关于页。
 *
 * 结构按「先品牌、后信息」组织：顶部是应用图标与名称，下面用列表逐条给出应用是什么、
 * 怎么用、隐私与免责说明，最后是致谢（开源项目与其仓库地址）。
 */
@Composable
fun AboutRoute(
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    AboutScreen(appVersion = appVersion, modifier = modifier)
}

@Composable
fun AboutScreen(
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // 直接取系统的启动图标：这样关于页永远和桌面上的图标是同一个，不需要维护第二份资源。
    val appIcon = remember(context) {
        runCatching { context.packageManager.getApplicationIcon(context.packageName) }.getOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIconBadge(drawable = appIcon, size = 80.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "闪光摩斯",
                style = MiuixTheme.textStyles.title1,
                color = colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "版本 $appVersion",
                style = MiuixTheme.textStyles.footnote2,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SmallTitle(text = "关于")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                AboutEntry(
                    title = "它是什么",
                    body = "把手机闪光灯变成一台可校准的摩斯电码发报机。全程离线，不需要网络、不需要配对，" +
                        "只要对方能看见光，就能收到报文。",
                )
                AboutEntry(
                    title = "怎么发报",
                    body = "输入文本后点按发送卡即可自动发报；也可以用手动点按逐符号敲击，" +
                        "或在紧急情况下循环发送 SOS。速度 5–25 WPM，单位时长与时序补偿都在设置页调节。",
                )
                AboutEntry(
                    title = "两种发光通道",
                    body = "闪光灯通道亮度高、指向性强；15 WPM 以上的高速发报会自动切到屏幕闪光，" +
                        "因为 LED 的硬件延迟在该速度下无法保证点划比例。",
                )
                AboutEntry(
                    title = "隐私",
                    body = "本应用不申请网络权限，不采集任何数据。报文、短语与发送记录仅保存在本机，卸载即清除。",
                )
                AboutEntry(
                    title = "免责声明",
                    body = "SOS 是辅助手段，不能替代专业救援设备与 112 / 110 报警。" +
                        "请仅在真实的紧急情况下使用求救信号；滥用求救信号可能承担法律责任。",
                    showDivider = false,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SmallTitle(text = "致谢")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                AboutLink(
                    title = "Miuix",
                    body = "界面使用的 HyperOS 风格 Compose 组件库，本应用的配色、卡片与页面骨架都来自它。",
                    url = MIUIX_REPO,
                    onClick = { uriHandler.openUri(MIUIX_REPO) },
                )
                AboutEntry(
                    title = "miuix-icons",
                    body = "同一项目提供的官方图标集，应用内的设置、返回、添加等图标均取自这里。",
                    showDivider = false,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 应用图标：自适应图标本身是方形画布，这里统一裁成与桌面一致的圆角外形。 */
@Composable
private fun AppIconBadge(
    drawable: android.graphics.drawable.Drawable?,
    size: androidx.compose.ui.unit.Dp,
) {
    val shape = RoundedCornerShape(size * 0.26f)

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (drawable != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    drawable.setBounds(0, 0, this.size.width.toInt(), this.size.height.toInt())
                    drawable.draw(canvas.nativeCanvas)
                }
            }
        } else {
            Icon(
                imageVector = FmIcons.Send,
                contentDescription = null,
                modifier = Modifier.size(size * 0.42f),
                tint = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AboutEntry(
    title: String,
    body: String,
    showDivider: Boolean = true,
) {
    val colorScheme = MiuixTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.subtitle,
            color = colorScheme.onSurfaceContainer,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            style = MiuixTheme.textStyles.footnote1,
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 0.dp),
                color = colorScheme.dividerLine,
            )
        }
    }
}

/** 致谢条目：整行可点，直接打开仓库地址；URL 原样显示，便于抄写或分享。 */
@Composable
private fun AboutLink(
    title: String,
    body: String,
    url: String,
    onClick: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.subtitle,
                    color = colorScheme.onSurfaceContainer,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MiuixTheme.textStyles.footnote1,
                    color = colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = url,
                    style = MiuixTheme.textStyles.footnote2,
                    color = colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = FmIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colorScheme.onSurfaceVariantActions,
            )
        }
        HorizontalDivider(color = colorScheme.dividerLine)
    }
}
