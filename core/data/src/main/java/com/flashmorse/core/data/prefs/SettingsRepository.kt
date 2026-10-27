package com.flashmorse.core.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flashmorse.core.model.Brightness
import com.flashmorse.core.model.ChannelPreference
import com.flashmorse.core.model.TimingProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "flashmorse_settings",
)

/**
 * 设置的读写实现（DataStore Preferences）。
 *
 * 键值设置用 DataStore、结构化数据用 JSON 文件，二者均在 [core:data] 内，
 * 上层只依赖 Repository 接口语义。
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<Settings> = dataStore.data
        .map { it.toSettings() }
        .distinctUntilChanged()

    suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { prefs ->
            val current = prefs.toSettings()
            val next = transform(current)
            if (next == current) return@edit
            prefs[Keys.Wpm] = next.wpm.coerceIn(TimingProfile.MIN_WPM, TimingProfile.MAX_WPM)
            prefs[Keys.Channel] = next.channel.name
            prefs[Keys.Haptic] = next.hapticFeedback
            prefs[Keys.Loop] = next.loopByDefault
            prefs[Keys.Compensation] = next.compensationMillis
            prefs[Keys.LastMessage] = next.lastMessage
            prefs[Keys.Brightness] = Brightness.clampPercent(next.brightnessPercent)
            prefs[Keys.RetainBrightness] = next.retainTorchBrightness
            prefs[Keys.SoundEnabled] = next.soundEnabled
        }
    }

    suspend fun setWpm(wpm: Int) = update { it.copy(wpm = wpm.coerceIn(TimingProfile.MIN_WPM, TimingProfile.MAX_WPM)) }

    suspend fun setChannel(channel: ChannelPreference) = update { it.copy(channel = channel) }

    suspend fun setHaptic(enabled: Boolean) = update { it.copy(hapticFeedback = enabled) }

    suspend fun setLoop(enabled: Boolean) = update { it.copy(loopByDefault = enabled) }

    suspend fun setCompensation(millis: Int) = update { it.copy(compensationMillis = millis.coerceIn(-60, 60)) }

    suspend fun setLastMessage(text: String) = update { it.copy(lastMessage = text) }

    suspend fun setRetainBrightness(retain: Boolean) = update {
        it.copy(retainTorchBrightness = retain)
    }

    suspend fun setSoundEnabled(enabled: Boolean) = update {
        it.copy(soundEnabled = enabled)
    }

    suspend fun setBrightness(percent: Int) = update {
        it.copy(brightnessPercent = Brightness.clampPercent(percent))
    }

    private fun Preferences.toSettings(): Settings = Settings(
        wpm = this[Keys.Wpm] ?: TimingProfile.DEFAULT_WPM,
        channel = this[Keys.Channel]?.let { name ->
            ChannelPreference.entries.firstOrNull { it.name == name }
        } ?: ChannelPreference.Auto,
        hapticFeedback = this[Keys.Haptic] ?: true,
        loopByDefault = this[Keys.Loop] ?: false,
        compensationMillis = this[Keys.Compensation] ?: 0,
        lastMessage = this[Keys.LastMessage] ?: "",
        brightnessPercent = Brightness.clampPercent(
            this[Keys.Brightness] ?: Brightness.DEFAULT_PERCENT,
        ),
        retainTorchBrightness = this[Keys.RetainBrightness] ?: true,
        soundEnabled = this[Keys.SoundEnabled] ?: false,
    )

    private object Keys {
        val Wpm = intPreferencesKey("wpm")
        val Channel = stringPreferencesKey("channel")
        val Haptic = booleanPreferencesKey("haptic")
        val Loop = booleanPreferencesKey("loop")
        val Compensation = intPreferencesKey("compensation_ms")
        val LastMessage = stringPreferencesKey("last_message")
        val Brightness = intPreferencesKey("brightness_percent")
        val RetainBrightness = booleanPreferencesKey("retain_torch_brightness")
        val SoundEnabled = booleanPreferencesKey("sound_enabled")
    }
}
