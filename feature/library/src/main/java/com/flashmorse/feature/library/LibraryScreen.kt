package com.flashmorse.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.component.MorsePattern
import com.flashmorse.core.designsystem.component.SectionHeader
import com.flashmorse.core.designsystem.icon.FmIcons
import com.flashmorse.core.model.HistoryEntry
import com.flashmorse.core.model.Phrase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Card
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
 */
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onTabSelected: (Int) -> Unit,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    onNewPhrase: () -> Unit,
    onPhraseLongPress: (Phrase) -> Unit,
    onHistoryDelete: (String) -> Unit,
    onHistoryClear: () -> Unit,
    onEditorLabelChange: (String) -> Unit,
    onEditorPayloadChange: (String) -> Unit,
    onEditorSave: () -> Unit,
    onEditorDismiss: () -> Unit,
    onMenuDismiss: () -> Unit,
    onEdit: (Phrase) -> Unit,
    onDelete: (Phrase) -> Unit,
    onMove: (Phrase, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme

    // 历史记录是「删了就没有」的数据，两个入口（单条删除 / 清空）都先过一遍确认。
    var pendingDelete by remember { mutableStateOf<HistoryEntry?>(null) }
    var pendingClear by remember { mutableStateOf(false) }

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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 104.dp),
                ) {
                    item {
                        Column {
                            SectionHeader(text = "短语库")
                            // 操作提示紧跟在分组标题下方：它说明的是「这一整块列表怎么用」，
                            // 放在列表末尾既容易被 FAB 挤开，也要滚到底才看得到。
                            Text(
                                text = "点按立即发送 · 长按管理",
                                style = MiuixTheme.textStyles.footnote2,
                                color = colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(start = 28.dp, end = 28.dp),
                            )
                        }
                    }
                    if (state.phrases.isEmpty()) {
                        item { EmptyHint("还没有短语，点右下角 + 新建一条") }
                    }
                    items(state.phrases, key = { it.phrase.id }) { item ->
                        PhraseRow(
                            item = item,
                            onClick = { onPlay(item.phrase.payload) },
                            onLongPress = { onPhraseLongPress(item.phrase) },
                        )
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
                            onDelete = { pendingDelete = entry },
                        )
                    }
                }
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

        if (state.isPlaying) {
            PlayingBanner(
                text = state.playingText ?: "",
                onStop = onStop,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
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

    state.menuTarget?.let { phrase ->
        OverlayDialog(
            title = phrase.label,
            summary = phrase.payload,
            show = true,
            onDismissRequest = onMenuDismiss,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        text = "上移",
                        onClick = { onMove(phrase, -1) },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = "下移",
                        onClick = { onMove(phrase, 1) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        text = "编辑",
                        onClick = { onEdit(phrase) },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = "删除",
                        onClick = { onDelete(phrase) },
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(
                    text = "取消",
                    onClick = onMenuDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    pendingDelete?.let { entry ->
        HistoryConfirmDialog(
            title = "删除这条记录？",
            summary = "「${entry.text}」将从发送记录中移除，无法恢复。",
            confirmLabel = "删除",
            onConfirm = {
                onHistoryDelete(entry.id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (pendingClear) {
        HistoryConfirmDialog(
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
private fun HistoryConfirmDialog(
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
        TextButton(
            text = confirmLabel,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            text = "取消",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// -------------------------------------------------------------------- 列表行

@Composable
private fun PhraseRow(
    item: PhraseItem,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
        onLongPress = onLongPress,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    // 点划预览紧贴标题，形成「中文名 + 电码形态」的一组信息
                    if (item.previewPattern.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        MorsePattern(
                            pattern = item.previewPattern,
                            modifier = Modifier
                                .width(52.dp)
                                .height(6.dp),
                            height = 6.dp,
                            color = colorScheme.onSurfaceContainerVariant,
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

            Icon(
                imageVector = FmIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colorScheme.onSurfaceVariantActions,
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
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
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
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = FmIcons.Delete,
                    contentDescription = "删除记录",
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariantActions,
                )
            }
        }
    }
}

@Composable
private fun PlayingBanner(
    text: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "正在发送",
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onStop) {
                Icon(
                    imageVector = FmIcons.Close,
                    contentDescription = "停止发送",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.primary,
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
        TextButton(
            text = "保存",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
        )
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
