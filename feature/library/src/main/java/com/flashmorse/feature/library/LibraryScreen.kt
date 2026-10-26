package com.flashmorse.feature.library

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.flashmorse.core.designsystem.component.MorsePattern
import com.flashmorse.core.designsystem.component.SectionHeader
import com.flashmorse.core.designsystem.icon.FmIcons
import com.flashmorse.core.model.HistoryEntry
import com.flashmorse.core.model.Phrase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 短语库与历史（F-09 / F-10）。
 *
 * 列表项的主操作是「立即发送」而不是「载入到发报页」——这一页的典型用途是
 * 隔河/隔街的一次性暗号，多一次跳转就多一次失败机会。
 *
 * 行内操作布局：`[编辑] 内容 [删除]`，两侧都是无背景的实色 Miuix 图标；
 * 排序为**长按拖拽**，不再有长按菜单。
 */
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onTabSelected: (Int) -> Unit,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    onNewPhrase: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    onHistoryDelete: (String) -> Unit,
    onHistoryClear: () -> Unit,
    onEditorLabelChange: (String) -> Unit,
    onEditorPayloadChange: (String) -> Unit,
    onEditorSave: () -> Unit,
    onEditorDismiss: () -> Unit,
    onEdit: (Phrase) -> Unit,
    onDelete: (Phrase) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme

    // 删除是不可逆操作：短语单条 / 历史单条 / 历史清空都先过一遍确认。
    var pendingPhraseDelete by remember { mutableStateOf<Phrase?>(null) }
    var pendingHistoryDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    var pendingClear by remember { mutableStateOf(false) }

    // ---- 拖拽排序（长按拖动）----
    val listState = rememberLazyListState()
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val phrasesState by rememberUpdatedState(state.phrases)
    val currentOnReorder by rememberUpdatedState(onReorder)

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(6.dp))
        TabRow(
            tabs = listOf("短语", "历史"),
            selectedTabIndex = if (state.tab == LibraryTab.Phrases) 0 else 1,
            onTabSelected = onTabSelected,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            when (state.tab) {
                LibraryTab.Phrases -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 104.dp),
                ) {
                    item {
                        SectionHeader(
                            text = "短语库",
                            trailing = {
                                Text(
                                    text = "点击发送，长按拖动排序",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                            },
                        )
                    }
                    if (state.phrases.isEmpty()) {
                        item { EmptyHint("还没有短语，点右下角 + 新建一条") }
                    }
                    items(state.phrases, key = { it.phrase.id }) { item ->
                        val dragging = item.phrase.id == draggingKey
                        // 拖拽手势挂在行的内容层（叶子节点）：Miuix Card 的按压反馈
                        // 也是事件消费者，挂在更上层会被它的 Main-pass 处理抢在前面。
                        val dragModifier = Modifier.pointerInput(item.phrase.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingKey = item.phrase.id
                                    dragOffsetPx = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dragged = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.key == item.phrase.id }
                                    if (dragged == null) {
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    dragOffsetPx += dragAmount.y
                                    val centerY = dragged.offset + dragged.size / 2 + dragOffsetPx
                                    // 换位阈值是**目标项的中点**而不是边界：刚碰到目标项就换位
                                    // 会在中点附近来回抖动（视觉上的"抽搐"）
                                    val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                                        it.key is String &&
                                            it.key != item.phrase.id &&
                                            centerY.toInt() >= it.offset &&
                                            centerY.toInt() < it.offset + it.size
                                    }
                                    val passedMidpoint = target != null && run {
                                        val targetCenter = target.offset + target.size / 2
                                        if (target.offset > dragged.offset) {
                                            centerY >= targetCenter // 向下拖：越过目标中点才换
                                        } else {
                                            centerY <= targetCenter // 向上拖：同理
                                        }
                                    }
                                    if (passedMidpoint) {
                                        val from = phrasesState.indexOfFirst { it.phrase.id == item.phrase.id }
                                        val to = phrasesState.indexOfFirst { it.phrase.id == target.key }
                                        if (from >= 0 && to >= 0 && from != to) {
                                            currentOnReorder(from, to)
                                            // 换位后补回偏移，保持被拖项始终跟手
                                            dragOffsetPx -= (target.offset - dragged.offset)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingKey = null
                                    dragOffsetPx = 0f
                                },
                                onDragCancel = {
                                    draggingKey = null
                                    dragOffsetPx = 0f
                                },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .animateItem()
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    if (dragging) translationY = dragOffsetPx
                                },
                        ) {
                            PhraseRow(
                                item = item,
                                dragModifier = dragModifier,
                                onClick = { onPlay(item.phrase.payload) },
                                onEdit = { onEdit(item.phrase) },
                                onDelete = { pendingPhraseDelete = item.phrase },
                            )
                        }
                    }
                }

                LibraryTab.History -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 104.dp),
                ) {
                    item { SectionHeader(text = "最近发送") }
                    if (state.history.isEmpty()) {
                        item { EmptyHint("还没有发送记录") }
                    }
                    items(state.history, key = { it.id }) { entry ->
                        HistoryRow(
                            entry = entry,
                            onClick = { onPlay(entry.text) },
                            onDelete = { pendingHistoryDelete = entry },
                        )
                    }
                }
            }

            // 播放状态卡片：覆盖在列表正中央；投影 + 高一档的表面色提供层次感
            if (state.isPlaying) {
                PlayingOverlay(
                    text = state.playingText ?: "",
                    onStop = onStop,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // 两个板块共用同一个右下角主操作位：短语=新建，历史=清空
            when {
                state.tab == LibraryTab.Phrases -> {
                    FloatingActionButton(
                        onClick = onNewPhrase,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 20.dp),
                    ) {
                        Icon(
                            imageVector = FmIcons.Add,
                            contentDescription = "新建短语",
                            tint = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
                }

                state.history.isNotEmpty() -> {
                    FloatingActionButton(
                        onClick = { pendingClear = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 20.dp, bottom = 20.dp),
                    ) {
                        Icon(
                            imageVector = FmIcons.Delete,
                            contentDescription = "清空发送记录",
                            tint = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }

    state.editor?.let { editor ->
        PhraseEditorDialog(
            editor = editor,
            onLabelChange = onEditorLabelChange,
            onPayloadChange = onEditorPayloadChange,
            onSave = onEditorSave,
            onDismiss = onEditorDismiss,
        )
    }

    pendingPhraseDelete?.let { phrase ->
        DeleteConfirmDialog(
            title = "删除这条短语？",
            summary = "「${phrase.label}」将从短语库中移除，无法恢复。",
            confirmLabel = "删除",
            onConfirm = {
                onDelete(phrase)
                pendingPhraseDelete = null
            },
            onDismiss = { pendingPhraseDelete = null },
        )
    }

    pendingHistoryDelete?.let { entry ->
        DeleteConfirmDialog(
            title = "删除这条记录？",
            summary = "「${entry.text}」将从发送记录中移除，无法恢复。",
            confirmLabel = "删除",
            onConfirm = {
                onHistoryDelete(entry.id)
                pendingHistoryDelete = null
            },
            onDismiss = { pendingHistoryDelete = null },
        )
    }

    if (pendingClear) {
        DeleteConfirmDialog(
            title = "清空发送记录？",
            summary = "共 ${state.history.size} 条记录将被删除，无法恢复。",
            confirmLabel = "清空",
            onConfirm = {
                onHistoryClear()
                pendingClear = false
            },
            onDismiss = { pendingClear = false },
        )
    }
}

/**
 * 删除确认弹窗，用 Miuix 的 [OverlayDialog]。
 *
 * 不自己拼一个全屏遮罩：OverlayDialog 已经处理了遮罩层级、返回键与出入场，
 * 与短语编辑、SOS 免责保持同一套弹层语汇。
 */
@Composable
private fun DeleteConfirmDialog(
    title: String,
    summary: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        title = title,
        summary = summary,
        show = true,
        onDismissRequest = onDismiss,
    ) {
        // 左取消 / 右删除：破坏性操作固定放在最右侧，位置与系统对话框习惯一致
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.error,
                    contentColor = MiuixTheme.colorScheme.onError,
                ),
            ) {
                Text(
                    text = confirmLabel,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onError,
                )
            }
        }
    }
}

// -------------------------------------------------------------------- 列表行

@Composable
private fun PhraseRow(
    item: PhraseItem,
    dragModifier: Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(dragModifier)
                .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowIconAction(
                icon = FmIcons.Edit,
                label = "编辑",
                tint = colorScheme.primary,
                onClick = onEdit,
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.phrase.label,
                        style = MiuixTheme.textStyles.subtitle,
                        color = colorScheme.onSurfaceContainer,
                    )
                    if (item.phrase.builtIn) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "内置",
                            style = MiuixTheme.textStyles.footnote2,
                            color = colorScheme.primary,
                        )
                    }
                    // 点划预览紧贴标题；宽度按报文长度自适应，SOS 这类短报文完整显示
                    if (item.previewPattern.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        // 超出可视宽度的电码可横向拖动查看（如 NEED HELP）
                        MorsePattern(
                            pattern = item.previewPattern,
                            modifier = Modifier
                                .widthIn(max = 140.dp)
                                .height(5.dp),
                            height = 5.dp,
                            color = colorScheme.onSurfaceContainerVariant,
                            scrollable = true,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.phrase.payload,
                    style = MiuixTheme.textStyles.footnote1,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }

            RowIconAction(
                icon = FmIcons.Delete,
                label = "删除",
                tint = colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        insideMargin = PaddingValues(0.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.text,
                    style = MiuixTheme.textStyles.subtitle,
                    color = colorScheme.onSurfaceContainer,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatTime(entry.sentAtEpochMillis)} · ${entry.wpm} WPM · ${entry.channel.displayName}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }

            RowIconAction(
                icon = FmIcons.Delete,
                label = "删除",
                tint = colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

/** 行内图标操作：无背景的实色 Miuix 图标，颜色即语义（蓝 = 编辑、红 = 删除）。 */
@Composable
private fun RowIconAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
    }
}

/**
 * 发送中的状态卡片：覆盖在列表正中央，纵向排布——
 * 上方是「正在发送」与报文文本，下方是停止按钮。
 * 投影 + 比列表高一档的表面色，保证与下层内容有清晰层次。
 */
@Composable
private fun PlayingOverlay(
    text: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier
            .shadow(elevation = 16.dp, shape = shape)
            // 描边把悬浮卡与列表卡的边界拉开：阴影负责"浮起"，描边负责"分层"
            .border(width = 1.dp, color = colorScheme.dividerLine, shape = shape)
            .width(260.dp),
        cornerRadius = 24.dp,
        colors = CardDefaults.defaultColors(color = colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "正在发送",
                style = MiuixTheme.textStyles.subtitle,
                color = colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                style = MiuixTheme.textStyles.footnote1,
                color = colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onStop,
                modifier = Modifier.size(52.dp),
                cornerRadius = 26.dp,
                colors = ButtonDefaults.buttonColors(
                    color = colorScheme.error,
                    contentColor = colorScheme.onError,
                ),
            ) {
                Icon(
                    imageVector = FmIcons.Close,
                    contentDescription = "停止发送",
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 28.dp),
        textAlign = TextAlign.Center,
    )
}

// ---------------------------------------------------------------------- 编辑

@Composable
private fun PhraseEditorDialog(
    editor: PhraseEditor,
    onLabelChange: (String) -> Unit,
    onPayloadChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 用 state-based TextField（TextFieldState）：selection 由 state 自己拥有，
    // 点击文本中间后光标不会被外部重组重置到行尾。
    // 实测 Miuix 的 TextFieldValue 重载在同样场景下仍会丢 selection，故不用它。
    val labelState = rememberTextFieldState(editor.label)
    val payloadState = rememberTextFieldState(editor.payload)

    LaunchedEffect(labelState.text) { onLabelChange(labelState.text.toString()) }
    LaunchedEffect(payloadState.text) { onPayloadChange(payloadState.text.toString()) }

    OverlayDialog(
        title = if (editor.isNew) "新建短语" else "编辑短语",
        show = true,
        onDismissRequest = onDismiss,
    ) {
        TextField(
            state = labelState,
            label = "名称（可选）",
            useLabelAsPlaceholder = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            state = payloadState,
            label = "报文（A–Z / 0–9 / 标点）",
            useLabelAsPlaceholder = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Text(
                text = "保存",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            text = "取消",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// --------------------------------------------------------------------- helpers

private fun formatTime(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L} 分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L} 小时前"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }
}
