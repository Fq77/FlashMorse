package com.flashmorse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 停止提示的颜色：白底上对比度 4.3:1、黑底上 4.9:1，明灭切换时不会"闪一下又看不见"。 */
private val StopHintColor = Color(0xFF7A7A7A)

/**
 * 屏幕闪光通道的全屏输出层（规划文档 §6.1 方案 D）。
 *
 * 设计约束：
 * 1. **整屏发光**：光斑面积越大，可被看见的距离越远，这是这个通道存在的意义；
 *    代价是 OLED 长时间全白有烧屏风险（风险 R9），因此该通道主要用于短报文与 SOS，
 *    长时间发送应交给 LED 通道。
 * 2. 亮/灭是整屏切换：亮时全屏白、灭时全屏黑，不保留任何常亮的装饰元素，
 *    否则熄灭间隙会露出界面，既破坏对比度，也让观感变得躁动。
 * 3. 屏幕通道**不参与亮度调节**：它的价值在于"最亮、最容易被看见"，
 *    调暗只会削弱唯一的优势；亮度设定只作用于手电筒。
 * 4. 亮灭状态**只在绘制阶段读取**（见 drawBehind 的延迟读取）：灯的每次通断
 *    只重绘这一层，不触发任何重组——此前把亮灭作为组合期状态收集在装配层，
 *    每次通断都重组整棵页面树，主线程一掉帧灯的边沿就错乱（表现为乱闪）。
 *
 * 该层同时是「停止」入口：全屏输出时用户看不到底部导航，必须就地提供明确的停止手势。
 */
@Composable
fun ScreenFlashOverlay(
    active: Boolean,
    litState: State<Boolean>,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!active) return

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onStop,
            )
            .drawBehind {
                // litState.value 是快照状态：在绘制阶段读取意味着变化只触发重绘，
                // 组合与布局完全不参与——时序线程的通断事件逐帧精确呈现
                drawRect(if (litState.value) Color.White else Color.Black)
            },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "屏幕闪光发送中 · 点按任意位置停止",
                style = MiuixTheme.textStyles.footnote1,
                color = StopHintColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
