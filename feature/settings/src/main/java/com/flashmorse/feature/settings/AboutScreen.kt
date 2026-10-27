package com.flashmorse.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.icon.FmIcons
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Miuix 开源项目地址：致谢栏与「设计与许可」共用同一常量。 */
private const val MIUIX_REPO = "https://github.com/compose-miuix-ui/miuix"

/** 作者主页地址。 */
private const val AUTHOR_REPO = "https://github.com/Fq77"

/**
 * 关于页。
 *
 * 结构按「先品牌、后信息」组织：顶部是应用图标与名称，下面用列表逐条给出应用是什么、
 * 怎么用、隐私与免责说明，最后分「作者」「致谢」两个独立区块。
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

        SmallTitle(text = "作者")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                AboutLink(
                    avatarRes = com.flashmorse.core.designsystem.R.drawable.avatar_fq77,
                    title = "Eric Feng",
                    body = "闪光摩斯的作者",
                    url = AUTHOR_REPO,
                    onClick = { uriHandler.openUri(AUTHOR_REPO) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SmallTitle(text = "致谢")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                AboutLink(
                    avatarRes = com.flashmorse.core.designsystem.R.drawable.avatar_miux_author,
                    title = "YuKongA · Miuix",
                    body = "HyperOS 风格的 Compose 组件库：本应用的配色、卡片、图标与页面骨架都来自它。",
                    url = MIUIX_REPO,
                    onClick = { uriHandler.openUri(MIUIX_REPO) },
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
            HorizontalDivider(color = colorScheme.dividerLine)
        }
    }
}

/**
 * 致谢条目：头像 + 名称 + 说明 + 仓库地址，整行可点。
 * 头像统一圆形裁剪（GitHub 头像方图 → CircleShape）。
 */
@Composable
private fun AboutLink(
    avatarRes: Int,
    title: String,
    body: String,
    url: String,
    onClick: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(avatarRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
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
}
