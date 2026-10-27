package com.flashmorse.feature.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.icon.FmIcons
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 发报页：页面的唯一视觉焦点是中间的大发送卡。
 *
 * 自上而下：报文与符号流 → 大发送卡 → 一键操作。速度只在设置页调节，此处不重复。
 *
 * 这个文件只保留「页面骨架 + 一键操作 + 免责确认」；
 * 发送卡在 [SendCard]，报文输入在 [MessageInput]，手电筒整页在 [TorchScreen]。
 */
@Composable
fun SenderScreen(
    state: SenderUiState,
    onMessageChange: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onSosClick: () -> Unit,
    onOpenManual: () -> Unit,
    onToggleLight: () -> Unit,
    onTorchBrightnessPreview: (Int) -> Unit,
    onTorchBrightnessCommit: (Int) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 常亮即"手电筒模式"：整页切换成灯面 + 调光，不再显示报文与发送卡，
    // 否则用户分不清当前处于哪种状态。
    if (state.constantLightOn) {
        TorchScreen(
            state = state,
            onPreview = onTorchBrightnessPreview,
            onCommit = onTorchBrightnessCommit,
            onTurnOff = onToggleLight,
        )
        return
    }

    val scrollState = rememberScrollState()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // 整页可滚动 + 组件使用固定高度：
    // 输入法弹出时窗口变矮，视口随之变小，但任何组件都不会被压缩、隐藏或跳位，
    // 用户只是需要向上滚动才能看到下方的发送区——这是安卓里唯一能同时满足
    //「不挤压 / 不隐藏 / 布局稳定」三者的做法（其余方案必然改变某处尺寸）。
    Column(
        modifier = modifier
            .fillMaxSize()
            // edge-to-edge 下 adjustResize 不再自动缩小窗口，必须显式消费 IME inset：
            // 只收缩「滚动视口」，组件自身的尺寸与相对位置保持不变，因此没有挤压或跳位，
            // 被键盘挡住的发送区可以通过滚动到达。
            .imePadding()
            .padding(horizontal = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MessageInput(state = state, onMessageChange = onMessageChange)

        SendCard(
            state = state,
            onClick = onPrimaryAction,
            modifier = Modifier
                .fillMaxWidth()
                // 横屏视口高度可能不足 360 dp，340 dp 的竖排卡会独占一屏并把操作区顶到屏幕外。
                // 横屏改用 200 dp，正好落进 SendCard 内部的 compact（< 208 dp）分支。
                .height(if (landscape) SEND_CARD_LANDSCAPE_HEIGHT else SEND_CARD_HEIGHT),
        )

        ActionRow(
            state = state,
            landscape = landscape,
            onSosClick = onSosClick,
            onOpenManual = onOpenManual,
            onToggleLight = onToggleLight,
            onStop = onStop,
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

// -------------------------------------------------------------------- 一键操作

@Composable
private fun ActionRow(
    state: SenderUiState,
    landscape: Boolean,
    onSosClick: () -> Unit,
    onOpenManual: () -> Unit,
    onToggleLight: () -> Unit,
    onStop: () -> Unit,
) {
    if (state.isSending) {
        // 暂停已移除：对光信号来说"暂停几秒"对接收方等于乱码，只保留明确的停止。
        // 实色胶囊按钮 + 红色：与"删除/中止"同一套色彩语义。
        // 必须带文字：屏幕正在闪的时候，用户扫一眼未必能确认这个图标就是停止。
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = onStop,
                modifier = Modifier.height(52.dp),
                cornerRadius = 26.dp,
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.error,
                    contentColor = MiuixTheme.colorScheme.onError,
                ),
            ) {
                Icon(
                    imageVector = FmIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "停止发送",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onError,
                )
            }
        }
        return
    }

    // SOS 是道德与安全底线入口，必须比「手动 / 手电筒」更重：与它们同尺寸、同字号，
    // 只有颜色不同（红 = 紧急）。
    if (landscape) {
        // 横屏省下纵向空间：三个入口并排一行，竖屏的"两行"布局会让操作区滑出屏幕。
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SosAction(onClick = onSosClick, modifier = Modifier.weight(1f))
            PrimaryAction(label = "手动发报", onClick = onOpenManual, modifier = Modifier.weight(1f))
            PrimaryAction(label = "手电筒常亮", onClick = onToggleLight, modifier = Modifier.weight(1f))
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SosAction(onClick = onSosClick, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryAction(
                label = "手动发报",
                onClick = onOpenManual,
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(
                label = "手电筒常亮",
                onClick = onToggleLight,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SosAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(ACTION_TILE_HEIGHT),
        colors = ButtonDefaults.buttonColors(
            color = MiuixTheme.colorScheme.error,
            contentColor = MiuixTheme.colorScheme.onError,
        ),
    ) {
        Text(
            text = "SOS",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onError,
        )
    }
}

/** 主操作实色按钮：Miuix 主题蓝，两个次级入口共用同一形态。 */
@Composable
private fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(ACTION_TILE_HEIGHT),
        colors = ButtonDefaults.buttonColorsPrimary(),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onPrimary,
        )
    }
}

/** 一行操作按钮的统一高度基准（SOS 与两个次级入口共用）。 */
private val ACTION_TILE_HEIGHT = 58.dp
