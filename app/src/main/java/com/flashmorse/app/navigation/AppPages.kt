package com.flashmorse.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.flashmorse.core.designsystem.icon.FmIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar

/**
 * 页面骨架。
 *
 * **为什么每个路由各自持有 Scaffold**：顶栏与底栏必须是页面内容的一部分，
 * 才能跟随 `NavDisplay` 的可预测返回手势一起位移。若把它们放在导航宿主外层，
 * 手势拖拽时只有正文响应、顶栏仍停在上一个页面的状态，底栏也要等栈真正弹出后才出现。
 *
 * Miuix 对 Scaffold 的约束是「一个页面边界一个 Scaffold」，这里每个路由正是一个页面边界，
 * 因此嵌套层级与规范一致；Scaffold 自身绘制 `surface` 背景，页面天然不透明。
 */
@Composable
fun HomePage(
    title: String,
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onOpenSettings: () -> Unit,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    // 手电筒开关：从页面按钮上移到全局可达的位置；
                    // 点亮态用主题色强调，一眼可辨当前灯态。
                    IconButton(onClick = onToggleTorch) {
                        Icon(
                            imageVector = FmIcons.Torch,
                            contentDescription = if (torchOn) "关闭手电筒" else "手电筒常亮",
                            tint = if (torchOn) {
                                top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary
                            } else {
                                top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground
                            },
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = FmIcons.Settings,
                            contentDescription = "设置",
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == selectedTab,
                        onClick = { onTabSelected(item) },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        },
        content = content,
    )
}

/** 二级页面骨架：只有返回 + 标题，无底栏。 */
@Composable
fun DetailPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = FmIcons.Back,
                            contentDescription = "返回",
                        )
                    }
                },
            )
        },
        content = content,
    )
}
