package com.flashmorse.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.icon.FmIcons
import kotlin.math.PI
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 屏幕闪光通道的全屏输出层（规划文档 §6.1 方案 D）。
 *
 * 两点设计约束：
 * 1. **只亮中心圆**，面积约占屏幕 40%，降低 OLED 长时间全白导致的烧屏风险（风险 R9）；
 * 2. 发送期间底色恒为黑，仅中心圆随信号明灭——若让底色跟随明灭，熄灭间隙会露出应用界面，
 *    既破坏信号对比度，也让观感变得躁动。
 *
 * 该层同时是「停止」入口：全屏输出时用户看不到底部导航，必须就地提供明确的停止手势。
 */
@Composable
fun ScreenFlashOverlay(
    active: Boolean,
    lit: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!active) return

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onStop,
            ),
    ) {
        if (lit) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 面积约束：πr² = 40% × w × h
                val targetArea = size.width * size.height * 0.4f
                val radius = sqrt(targetArea / PI.toFloat())
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = FmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF9A9A9A),
                )
                Text(
                    text = "屏幕闪光发送中 · 点按任意位置停止",
                    style = MiuixTheme.textStyles.footnote1,
                    color = Color(0xFF9A9A9A),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
