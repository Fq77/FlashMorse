package com.flashmorse.feature.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.torch.MorseTransmitter

/** 识读练习导航入口。进入即开一轮，离开时 ViewModel 会停掉当前播放。 */
@Composable
fun PracticeRoute(
    settingsRepository: SettingsRepository,
    transmitter: MorseTransmitter,
    modifier: Modifier = Modifier,
) {
    val viewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModel.factory(settingsRepository, transmitter),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.startRound() }

    PracticeScreen(
        state = state,
        onReplay = viewModel::replay,
        onSelect = viewModel::select,
        onNext = viewModel::next,
        onRestart = viewModel::startRound,
        modifier = modifier,
    )
}
