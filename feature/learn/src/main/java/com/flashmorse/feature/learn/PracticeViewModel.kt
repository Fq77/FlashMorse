package com.flashmorse.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.model.MorseLetter
import com.flashmorse.core.model.TimingProfile
import com.flashmorse.core.model.isActive
import com.flashmorse.core.morse.codec.MorseTable
import com.flashmorse.core.morse.timing.ScriptCompiler
import com.flashmorse.core.torch.MorseTransmitter
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 识读练习（看辨 / 听辨）。
 *
 * 只用字母表出题：数字与标点的点划长且相似度高，混在一起会把练习变成猜长度，
 * 达不到建立「节奏 → 字符」反射的目的。
 */
class PracticeViewModel(
    private val settingsRepository: SettingsRepository,
    private val transmitter: MorseTransmitter,
) : ViewModel() {

    private val _state = MutableStateFlow(PracticeUiState())
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    private var wpm = TimingProfile.DEFAULT_WPM
    private var compensation = 0
    private val random = Random(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                wpm = settings.wpm
                compensation = settings.compensationMillis
                _state.update {
                    it.copy(wpm = settings.wpm, soundEnabled = settings.soundEnabled)
                }
            }
        }
        viewModelScope.launch {
            transmitter.state.collect { playback ->
                val playing = playback.isActive
                if (playing != _state.value.playing) {
                    _state.update { it.copy(playing = playing) }
                }
            }
        }
    }

    /** 开始新一轮（或重新开始）。第一题在进入时就播。 */
    fun startRound() {
        transmitter.stop()
        _state.update {
            PracticeUiState(
                roundSize = it.roundSize,
                wpm = it.wpm,
                soundEnabled = it.soundEnabled,
            )
        }
        ask()
    }

    /** 重播当前题：答错后想再确认一遍节奏时用。 */
    fun replay() {
        val question = _state.value.question ?: return
        play(question)
    }

    fun select(letter: MorseLetter) {
        val current = _state.value
        if (current.answered || current.question == null) return

        val correct = letter.char == current.question.char
        transmitter.stop()
        _state.update {
            it.copy(
                selected = letter.char,
                answered = true,
                correctCount = if (correct) it.correctCount + 1 else it.correctCount,
                askedCount = it.askedCount + 1,
            )
        }
    }

    fun next() {
        val current = _state.value
        if (!current.answered) return
        if (current.askedCount >= current.roundSize) {
            _state.update { it.copy(finished = true) }
            return
        }
        ask()
    }

    override fun onCleared() {
        transmitter.stop()
        super.onCleared()
    }

    // ------------------------------------------------------------------ internals

    private fun ask() {
        val alphabet = MorseTable.alphabet
        if (alphabet.size < OPTION_COUNT) return

        // 候选池里不能有重复字符，否则同一题会出现两个正确答案
        val pool = alphabet.shuffled(random)
        val question = pool.first()
        val distractors = pool.drop(1).take(OPTION_COUNT - 1)
        val options = (distractors + question).shuffled(random)

        _state.update {
            it.copy(
                question = question,
                options = options,
                selected = null,
                answered = false,
            )
        }
        play(question)
    }

    private fun play(letter: MorseLetter) {
        val timing = TimingProfile.forWpm(wpm, compensation)
        val script = ScriptCompiler.compileSingle(letter.char, letter.pattern, timing)
        transmitter.play(
            script = script,
            loop = false,
            channel = transmitter.resolveChannel(timing),
        )
    }

    companion object {
        private const val OPTION_COUNT = 4

        fun factory(
            settingsRepository: SettingsRepository,
            transmitter: MorseTransmitter,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { PracticeViewModel(settingsRepository, transmitter) }
        }
    }
}
