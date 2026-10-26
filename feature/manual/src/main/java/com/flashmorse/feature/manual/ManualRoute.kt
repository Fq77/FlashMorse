package com.flashmorse.feature.manual

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.torch.MorseTransmitter

/** 手动发报页导航入口。 */
@Composable
fun ManualRoute(
    settingsRepository: SettingsRepository,
    transmitter: MorseTransmitter,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ManualViewModel = viewModel(
        factory = ManualViewModel.factory(settingsRepository, transmitter),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnMessage by rememberUpdatedState(onMessage)

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { currentOnMessage(it) }
    }

    ManualScreen(
        state = state,
        onPressStart = viewModel::onPressStart,
        onPressEnd = viewModel::onPressEnd,
        onBackspace = viewModel::onBackspace,
        onClear = viewModel::onClear,
        onSend = viewModel::onSend,
        modifier = modifier,
    )
}
