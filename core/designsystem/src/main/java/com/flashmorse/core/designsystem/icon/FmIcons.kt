package com.flashmorse.core.designsystem.icon

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Undo

/**
 * 语义化图标别名层。
 *
 * 全部图标来自 `miuix-icons`（HyperOS 官方图标集），本文件**不含任何自绘路径**，
 * 只做「业务语义 → Miuix 图标」的映射。好处是调用点读起来是 `FmIcons.Send`，
 * 不必在 Add 还是 AddCircle 之间反复纠结；同时保证全应用图标语汇与 Miuix 组件一致。
 */
object FmIcons {

    /** 发报 / 发送 */
    val Send: ImageVector get() = MiuixIcons.Send

    /** 短语库 */
    val Notes: ImageVector get() = MiuixIcons.Notes

    /** 对照表 */
    val Grid: ImageVector get() = MiuixIcons.GridView

    /** 设置 */
    val Settings: ImageVector get() = MiuixIcons.Settings

    /** 返回 */
    val Back: ImageVector get() = MiuixIcons.Back

    /** 关闭 / 停止 */
    val Close: ImageVector get() = MiuixIcons.Close

    /** 完成 */
    val Check: ImageVector get() = MiuixIcons.Ok

    /** 继续 */
    val Play: ImageVector get() = MiuixIcons.Play

    /** 暂停 */
    val Pause: ImageVector get() = MiuixIcons.Pause

    /** 新建 */
    val Add: ImageVector get() = MiuixIcons.Add

    /** 删除 */
    val Delete: ImageVector get() = MiuixIcons.Delete

    /** 退格 / 撤销 */
    val Undo: ImageVector get() = MiuixIcons.Undo

    /** 列表项箭头 */
    val ChevronRight: ImageVector get() = MiuixIcons.ChevronForward

    /** 信息 / 诊断 */
    val Info: ImageVector get() = MiuixIcons.Info

    /** 编辑 */
    val Edit: ImageVector get() = MiuixIcons.Edit
}
