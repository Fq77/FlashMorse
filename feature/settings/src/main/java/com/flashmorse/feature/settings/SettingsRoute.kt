package com.flashmorse.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.torch.MorseTransmitter
import com.flashmorse.core.torch.ThermalGuard

/** 设置首页导航入口。 */
@Composable
fun SettingsRoute(
    settingsRepository: SettingsRepository,
    transmitter: MorseTransmitter,
    thermalGuard: ThermalGuard,
    appVersion: String,
    onOpenCalibration: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            settingsRepository = settingsRepository,
            transmitter = transmitter,
            thermalGuard = thermalGuard,
            appVersion = appVersion,
        ),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onWpmChange = viewModel::onWpmChange,
        onRetainBrightnessChange = viewModel::onRetainBrightnessChange,
        onChannelChange = viewModel::onChannelChange,
        onLoopChange = viewModel::onLoopChange,
        onHapticChange = viewModel::onHapticChange,
        onOpenCalibration = onOpenCalibration,
        onOpenDiagnostics = onOpenDiagnostics,
        onOpenAbout = onOpenAbout,
        modifier = modifier,
    )
}
