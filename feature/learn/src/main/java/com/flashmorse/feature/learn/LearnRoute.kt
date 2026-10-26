package com.flashmorse.feature.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.torch.MorseTransmitter

/** 对照表导航入口。 */
@Composable
fun LearnRoute(
    settingsRepository: SettingsRepository,
    transmitter: MorseTransmitter,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LearnViewModel = viewModel(
        factory = LearnViewModel.factory(settingsRepository, transmitter),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnMessage by rememberUpdatedState(onMessage)

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { currentOnMessage(it) }
    }

    LearnScreen(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onSelect = viewModel::onSelect,
        onPreview = viewModel::onPreview,
        modifier = modifier,
    )
}
