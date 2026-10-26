package com.flashmorse.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 应用唯一的主题装配点（规划文档 §7.3 / 验收 Q-08）。
 *
 * 全局锁定深色：不读取系统设置、不提供切换入口、不启用 Monet 动态取色。
 * 理由不是审美，而是光学约束——夜间场景下一次亮屏即可破坏双方 20–30 分钟的暗适应。
 */
@Composable
fun FlashMorseTheme(content: @Composable () -> Unit) {
    // 锁深色：不读取系统设置，不提供切换，故不需要 remember key
    val controller = remember { ThemeController(ColorSchemeMode.Dark) }
    MiuixTheme(controller = controller, content = content)
}
