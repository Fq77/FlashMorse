package com.flashmorse.feature.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.data.repository.HistoryRepository
import com.flashmorse.core.torch.MorseTransmitter
import com.flashmorse.core.torch.ThermalGuard

/**
 * 发报页导航入口：只负责取 ViewModel 与收集状态，真正的 UI 在 [SenderScreen]。
 *
 * 这条边界是 UI 可测试性的前提——`*Screen.kt` 必须能在 Preview 中独立渲染。
 */
@Composable
fun SenderRoute(
    settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository,
    transmitter: MorseTransmitter,
    thermalGuard: ThermalGuard,
    onOpenManual: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SenderViewModel = viewModel(
        factory = SenderViewModel.factory(
            settingsRepository = settingsRepository,
            historyRepository = historyRepository,
            transmitter = transmitter,
            thermalGuard = thermalGuard,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnMessage by rememberUpdatedState(onMessage)

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { currentOnMessage(it) }
    }

    SenderScreen(
        state = state,
        onMessageChange = viewModel::onMessageChange,
        onPrimaryAction = viewModel::onPrimaryAction,
        onSosClick = viewModel::onSosClick,
        onSosConfirm = viewModel::onSosConfirm,
        onSosDismiss = viewModel::onSosDismiss,
        onOpenManual = onOpenManual,
        onToggleLight = viewModel::onToggleConstantLight,
        onTorchBrightnessPreview = viewModel::onTorchBrightnessPreview,
        onTorchBrightnessCommit = viewModel::onTorchBrightnessCommit,
        onStop = viewModel::onStop,
        modifier = modifier,
    )
}
