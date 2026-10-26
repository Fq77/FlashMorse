package com.flashmorse.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.torch.MorseTransmitter
import com.flashmorse.core.torch.ThermalGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 设置、校准与设备诊断（F-05 / F-11 / F-08）。 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val transmitter: MorseTransmitter,
    private val thermalGuard: ThermalGuard,
    appVersion: String,
) : ViewModel() {

    private val capabilities = transmitter.capabilities()

    private val _state = MutableStateFlow(
        SettingsUiState(
            ledAvailable = transmitter.ledAvailable,
            supportsStrength = transmitter.ledSupportsBrightness,
            maxStrengthLevel = capabilities?.maxStrengthLevel ?: 0,
            cameraId = capabilities?.cameraId,
            appVersion = appVersion,
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        wpm = settings.wpm,
                        unitMillis = settings.timingProfile.unitMillis,
                        channel = settings.channel,
                        loop = settings.loopByDefault,
                        haptic = settings.hapticFeedback,
                        compensationMillis = settings.compensationMillis,
                        brightnessPercent = settings.brightnessPercent,
                        retainTorchBrightness = settings.retainTorchBrightness,
                        soundEnabled = settings.soundEnabled,
                        loaded = true,
                    )
                }
            }
        }
        viewModelScope.launch {
            thermalGuard.level.collect { level ->
                _state.update { it.copy(thermalLevel = level) }
            }
        }
    }

    /**
     * 亮度只在空闲时生效（[MorseTransmitter.setBrightness] 内部拦截），
     * 这里只负责落盘，实际下发由 Application 级的 settings 收集器统一做。
     */
    fun onSoundEnabledChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    }

    fun onRetainBrightnessChange(retain: Boolean) {
        viewModelScope.launch { settingsRepository.setRetainBrightness(retain) }
    }

    fun onChannelChange(index: Int) {
        val channel = ChannelPreference.entries.getOrNull(index) ?: return
        viewModelScope.launch { settingsRepository.setChannel(channel) }
    }

    fun onLoopChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setLoop(enabled) }
    }

    fun onHapticChange(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHaptic(enabled) }
    }

    fun onCompensationChange(millis: Int) {
        viewModelScope.launch { settingsRepository.setCompensation(millis) }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            transmitter: MorseTransmitter,
            thermalGuard: ThermalGuard,
            appVersion: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(settingsRepository, transmitter, thermalGuard, appVersion)
            }
        }
    }
}
