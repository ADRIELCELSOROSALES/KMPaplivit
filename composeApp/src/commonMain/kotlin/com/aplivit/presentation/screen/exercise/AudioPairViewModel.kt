package com.aplivit.presentation.screen.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.AudioPairExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AudioPairUiState(
    val exercise: AudioPairExercise? = null,
    val flashState: Map<Int, FlashState?> = emptyMap(),
    val foundCorrect: Set<Int> = emptySet(),
    val isCompleted: Boolean = false
)

class AudioPairViewModel(
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AudioPairUiState())
    val state: StateFlow<AudioPairUiState> = _state

    fun loadExercise(exercise: AudioPairExercise) {
        _state.value = AudioPairUiState(exercise = exercise)
        val strings = stringsFor(progressRepository.getSelectedLanguage())
        viewModelScope.launch {
            tts.speakAndWait(strings.audioPairInstruction)
            tts.speakSyllable(exercise.target)
        }
    }

    fun playTarget() {
        _state.value.exercise?.let { tts.speakSyllable(it.target) }
    }

    fun onOptionTapped(index: Int) {
        val exercise = _state.value.exercise ?: return
        if (index in _state.value.foundCorrect) return
        if (_state.value.flashState.containsKey(index)) return
        if (_state.value.flashState.size >= 2) return

        val isCorrect = exercise.options[index].equals(exercise.target, ignoreCase = true)
        val newFlashState = _state.value.flashState +
            (index to if (isCorrect) FlashState.CORRECT else FlashState.INCORRECT)
        _state.value = _state.value.copy(flashState = newFlashState)

        if (newFlashState.size < 2) return

        viewModelScope.launch {
            delay(700)
            val strings = stringsFor(progressRepository.getSelectedLanguage())
            val allCorrect = newFlashState.values.all { it == FlashState.CORRECT }
            tts.speak(if (allCorrect) strings.correct else strings.tryAgain)
            if (allCorrect) {
                _state.value = _state.value.copy(
                    foundCorrect = newFlashState.keys,
                    isCompleted = true
                )
            } else {
                delay(1000)
                _state.value = _state.value.copy(flashState = emptyMap())
            }
        }
    }
}
