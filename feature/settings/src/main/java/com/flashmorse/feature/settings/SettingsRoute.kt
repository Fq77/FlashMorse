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

    // 持久化设置尚未首次读出时不渲染：否则速度滑杆会先以默认值出现，
    // 再动画滑到保存值——表现为「从最左侧 0 迅速滑到先前的值」
    if (!state.loaded) return

    SettingsScreen(
        state = state,
        onWpmChange = viewModel::onWpmChange,
        onRetainBrightnessChange = viewModel::onRetainBrightnessChange,
        onSoundEnabledChange = viewModel::onSoundEnabledChange,
        onChannelChange = viewModel::onChannelChange,
        onLoopChange = viewModel::onLoopChange,
        onHapticChange = viewModel::onHapticChange,
        onOpenCalibration = onOpenCalibration,
        onOpenDiagnostics = onOpenDiagnostics,
        onOpenAbout = onOpenAbout,
        modifier = modifier,
    )
}
