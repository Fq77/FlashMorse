package com.flashmorse.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.data.repository.HistoryRepository
import com.flashmorse.core.data.repository.PhraseRepository
import com.flashmorse.core.torch.MorseTransmitter

/** 短语库导航入口。 */
@Composable
fun LibraryRoute(
    phraseRepository: PhraseRepository,
    historyRepository: HistoryRepository,
    settingsRepository: SettingsRepository,
    transmitter: MorseTransmitter,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(
            phraseRepository = phraseRepository,
            historyRepository = historyRepository,
            settingsRepository = settingsRepository,
            transmitter = transmitter,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnMessage by rememberUpdatedState(onMessage)

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { currentOnMessage(it) }
    }

    LibraryScreen(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onPlay = viewModel::onPlay,
        onStop = viewModel::onStop,
        onNewPhrase = { viewModel.onEditorOpen(null) },
        onPhraseLongPress = viewModel::onMenuOpen,
        onHistoryDelete = viewModel::onHistoryDelete,
        onHistoryClear = viewModel::onHistoryClear,
        onEditorLabelChange = viewModel::onEditorLabelChange,
        onEditorPayloadChange = viewModel::onEditorPayloadChange,
        onEditorSave = viewModel::onEditorSave,
        onEditorDismiss = viewModel::onEditorDismiss,
        onMenuDismiss = viewModel::onMenuDismiss,
        onEdit = { viewModel.onEditorOpen(it) },
        onDelete = viewModel::onDelete,
        onMove = viewModel::onMove,
        modifier = modifier,
    )
}
