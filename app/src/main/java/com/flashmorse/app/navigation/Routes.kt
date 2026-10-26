package com.flashmorse.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.flashmorse.core.designsystem.icon.FmIcons
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 路由表。
 *
 * 一级页面（发报 / 短语 / 对照）通过 [HomeTab] 切换，不占路由层级；
 * 设置与设置内的三个二级项都是压栈路由，因此天然带返回语义。
 */
@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Home : Route

    @Serializable
    data object Manual : Route

    @Serializable
    data object Settings : Route

    /** 速度调节（设置页子页：滑杆用满宽度，调速说明也归位在这里）。 */
    @Serializable
    data object Speed : Route

    @Serializable
    data object Calibration : Route

    @Serializable
    data object Diagnostics : Route

    @Serializable
    data object About : Route

    /** 求生与求救指南（顶栏「关于」图标进入）。 */
    @Serializable
    data object Survival : Route
}

/** 底部导航的一级入口（设置已移到顶栏，不再占底栏）。 */
enum class HomeTab(
    val label: String,
    val title: String,
    val icon: ImageVector,
) {
    Sender("发报", "闪光摩斯", FmIcons.Send),
    Library("短语", "短语与历史", FmIcons.Notes),
    Learn("对照", "摩斯对照表", FmIcons.Grid),
}
