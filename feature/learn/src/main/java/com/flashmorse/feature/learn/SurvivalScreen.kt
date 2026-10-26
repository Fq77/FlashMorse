package com.flashmorse.feature.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.component.MorsePattern
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 求生与求救指南（顶栏「关于」图标进入）。
 *
 * 定位是**离线应急手册**：没有网络时也能查到怎么发求救信号、
 * 怎么让信号更容易被看到。全部静态文本，Miuix 的 SmallTitle + Card 列表风格。
 */
@Composable
fun SurvivalScreen(modifier: Modifier = Modifier) {
    val colorScheme = MiuixTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        SmallTitle(text = "求救信号")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SurvivalEntry(
                    title = "SOS",
                    body = "国际通用求救信号：三短、三长、三短。任何媒介都可以发——" +
                        "闪光灯、哨声、敲击、挥手，甚至地面摆石块。",
                    pattern = "...---...",
                    showDivider = true,
                )
                SurvivalEntry(
                    title = "三次法则",
                    body = "任何信号重复三次即表示求救：三堆火、三声哨、三闪光、三声喊。" +
                        "这是全球搜救人员的通用判读约定。",
                    showDivider = true,
                )
                SurvivalEntry(
                    title = "遮挡法发灯光信号",
                    body = "用手、布或硬纸板有节奏地遮挡常亮光源，即可发出点划信号——" +
                        "不需要会闪的手电也能发 SOS。",
                    showDivider = true,
                )
                SurvivalEntry(
                    title = "对空与对地",
                    body = "对飞机：双臂上举挥动成 Y 形表示「需要帮助」，" +
                        "单臂上举一臂下垂表示「就地停留」。地面信号要做得比需要的更大。",
                    showDivider = false,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SmallTitle(text = "野外求生技巧")
        Card {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SurvivalEntry(
                    title = "让信号被看见",
                    body = "选择开阔高地、林间空地或水边发信号；夜间灯光比声音传播得远，" +
                        "白天烟雾和反光镜更有效。",
                    showDivider = true,
                )
                SurvivalEntry(
                    title = "省电就是保命",
                    body = "手电与手机是生命线：平时低亮守候，发现可疑灯光或声音时" +
                        "再用高亮短促呼叫，避免长时间常亮耗尽电量。",
                    showDivider = true,
                )
                SurvivalEntry(
                    title = "留下移动标记",
                    body = "离开原地时沿途留下箭头石堆、折枝或布条，" +
                        "让搜救者能顺着你的方向追踪。",
                    showDivider = true,
                )
                SurvivalEntry(
                    title = "原地等待优先",
                    body = "已发出信号后，原地等待通常优于长途跋涉：" +
                        "集中保暖、节约饮水，把体力留给信号响应的那一刻。",
                    showDivider = true,
                )
                SurvivalEntry(
                    title = "免责声明",
                    body = "本页内容仅为应急参考，不能替代专业求生训练。" +
                        "真实遇险时优先拨打 110 / 112 / 12395（水上搜救）。",
                    showDivider = false,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 单条知识条目：标题 + 正文，可选电码示意与分隔线。 */
@Composable
private fun SurvivalEntry(
    title: String,
    body: String,
    pattern: String? = null,
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
        )
        if (pattern != null) {
            Spacer(modifier = Modifier.height(6.dp))
            MorsePattern(
                pattern = pattern,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                height = 7.dp,
                color = colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (showDivider) {
            HorizontalDivider(color = colorScheme.dividerLine)
        }
    }
}
