package com.flashmorse.feature.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.component.MorsePattern
import com.flashmorse.core.model.MorseLetter
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 识读练习页（F-14 的延伸）。
 *
 * 交互只有三步：看光（或听声）→ 四选一 → 下一题。
 * 答完立刻公布正确答案与点划形态——练习的目的是建立映射，不是为了考住用户。
 */
@Composable
fun PracticeScreen(
    state: PracticeUiState,
    onReplay: () -> Unit,
    onSelect: (MorseLetter) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MiuixTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        ScoreBar(state = state)

        if (state.finished) {
            ResultCard(state = state, onRestart = onRestart)
            Spacer(modifier = Modifier.height(24.dp))
            return@Column
        }

        QuestionCard(state = state, onReplay = onReplay)

        SmallTitle(text = if (state.answered) "正确答案" else "选出你读到的字符")

        OptionGrid(state = state, onSelect = onSelect)

        if (state.answered) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = if (state.askedCount >= state.roundSize) "查看结果" else "下一题",
                    style = MiuixTheme.textStyles.body1,
                    color = colorScheme.onPrimary,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ScoreBar(state: PracticeUiState) {
    val colorScheme = MiuixTheme.colorScheme

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "第 ${state.questionNumber} / ${state.roundSize} 题",
                style = MiuixTheme.textStyles.subtitle,
                color = colorScheme.onSurfaceContainer,
            )
            Text(
                text = "正确 ${state.correctCount} · ${state.accuracyPercent}%",
                style = MiuixTheme.textStyles.footnote1,
                color = if (state.answered && state.isCorrect) {
                    colorScheme.primary
                } else {
                    colorScheme.onSurfaceVariantSummary
                },
            )
        }
    }
}

/** 播放区：未答时只有一个问号与重播入口，已答时揭示字符与点划。 */
@Composable
private fun QuestionCard(
    state: PracticeUiState,
    onReplay: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val question = state.question ?: return

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.height(76.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.answered) question.char.toString() else "?",
                    style = MiuixTheme.textStyles.title1,
                    color = when {
                        !state.answered -> colorScheme.onSurfaceVariantSummary
                        state.isCorrect -> colorScheme.primary
                        else -> colorScheme.error
                    },
                )
            }

            if (state.answered) {
                MorsePattern(
                    pattern = question.pattern,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp),
                    height = 9.dp,
                    color = colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = question.display,
                    style = MiuixTheme.textStyles.footnote2,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            } else {
                Text(
                    text = if (state.soundEnabled) {
                        "注意听 600 Hz 滴声，节奏即点划"
                    } else {
                        "注意看闪烁节奏"
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                text = if (state.playing) "正在播放…" else "再播一次",
                onClick = { if (!state.playing) onReplay() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OptionGrid(
    state: PracticeUiState,
    onSelect: (MorseLetter) -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    val rows = state.options.chunked(2)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { letter ->
                    val isAnswer = state.answered && letter.char == state.question?.char
                    val isWrongPick = state.answered && letter.char == state.selected && !isAnswer

                    val container = when {
                        isAnswer -> colorScheme.primary
                        isWrongPick -> colorScheme.error
                        else -> colorScheme.secondaryContainer
                    }
                    val content = when {
                        isAnswer -> colorScheme.onPrimary
                        isWrongPick -> colorScheme.onError
                        else -> colorScheme.onSecondaryContainer
                    }

                    Button(
                        onClick = { if (!state.answered) onSelect(letter) },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = container,
                            contentColor = content,
                        ),
                    ) {
                        Text(
                            text = letter.char.toString(),
                            style = MiuixTheme.textStyles.title3,
                            color = content,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    state: PracticeUiState,
    onRestart: () -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "本轮结束",
                style = MiuixTheme.textStyles.title3,
                color = colorScheme.onSurfaceContainer,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${state.roundSize} 题答对 ${state.correctCount} 题",
                style = MiuixTheme.textStyles.subtitle,
                color = colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "正确率 ${state.accuracyPercent}% · ${verdict(state.accuracyPercent)}",
                style = MiuixTheme.textStyles.footnote1,
                color = colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = "再来一轮",
                    style = MiuixTheme.textStyles.body1,
                    color = colorScheme.onPrimary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "速度 ${state.wpm} WPM。想更快识别，先把速度降到 8 WPM 以下练稳节奏。",
                style = MiuixTheme.textStyles.footnote2,
                color = colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun verdict(percent: Int): String = when {
    percent >= 90 -> "已经能靠节奏识别，可以尝试提速"
    percent >= 70 -> "基本稳定，继续巩固易混淆字符"
    percent >= 40 -> "节奏还在建立，建议降速重练"
    else -> "先把字母表按组背熟再看光"
}
