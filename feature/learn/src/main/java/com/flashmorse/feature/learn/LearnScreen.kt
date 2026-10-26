package com.flashmorse.feature.learn

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.component.MorsePattern
import com.flashmorse.core.model.MorseLetter
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * 摩斯对照表（F-14）。
 *
 * 课堂演示场景要求「大字符 + 可试闪」，因此网格只负责速查，
 * 选中项在底部给出大字与点划形态，并可直接试闪。
 */
@Composable
fun LearnScreen(
    state: LearnUiState,
    onTabSelected: (Int) -> Unit,
    onSelect: (MorseLetter) -> Unit,
    onPreview: (MorseLetter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(6.dp))
        TabRow(
            tabs = listOf("字母", "数字", "标点"),
            selectedTabIndex = state.tab.ordinal,
            onTabSelected = onTabSelected,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.visible, key = { it.char }) { letter ->
                LetterCell(
                    letter = letter,
                    selected = state.selected?.char == letter.char,
                    onClick = { onSelect(letter) },
                )
            }
        }

        state.selected?.let { letter ->
            DetailCard(
                letter = letter,
                playing = state.playingChar != null,
                wpm = state.wpm,
                onPreview = { onPreview(letter) },
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun LetterCell(
    letter: MorseLetter,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
        cornerRadius = 14.dp,
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = letter.char.toString(),
                style = MiuixTheme.textStyles.title3,
                color = if (selected) colorScheme.primary else colorScheme.onSurfaceContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            MorsePattern(
                pattern = letter.pattern,
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp),
                height = 2.8.dp,
                color = if (selected) {
                    colorScheme.primary
                } else {
                    colorScheme.onSurfaceContainerVariant
                },
            )
        }
    }
}

@Composable
private fun DetailCard(
    letter: MorseLetter,
    playing: Boolean,
    wpm: Int,
    onPreview: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val dots = letter.pattern.count { it == '.' }
    val dashes = letter.pattern.count { it == '-' }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.char.toString(),
                    style = MiuixTheme.textStyles.title1,
                    color = colorScheme.onSurfaceContainer,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                MorsePattern(
                    pattern = letter.pattern,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    height = 9.dp,
                    color = colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = letter.display + " · $dots 点 $dashes 划 · $wpm WPM",
                    style = MiuixTheme.textStyles.footnote2,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }

            // 试闪与停止同钮：实色按钮（蓝 = 试闪、红 = 停止），与全局按钮色彩语义一致
            Button(
                onClick = onPreview,
                colors = if (playing) {
                    ButtonDefaults.buttonColors(
                        color = colorScheme.error,
                        contentColor = colorScheme.onError,
                    )
                } else {
                    ButtonDefaults.buttonColorsPrimary()
                },
            ) {
                Text(
                    text = if (playing) "停止" else "试闪",
                    style = MiuixTheme.textStyles.body1,
                    color = if (playing) colorScheme.onError else colorScheme.onPrimary,
                )
            }
        }
    }
}
