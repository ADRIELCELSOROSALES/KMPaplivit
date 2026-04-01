package com.aplivit.presentation.screen.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.TouchExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class FlashState { CORRECT, INCORRECT }

data class TouchUiState(
    val exercise: TouchExercise? = null,
    val flashState: Map<Int, FlashState?> = emptyMap(),
    val foundCorrect: Set<Int> = emptySet(),
    val isCompleted: Boolean = false
)

class TouchViewModel(
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TouchUiState())
    val state: StateFlow<TouchUiState> = _state

    fun loadExercise(exercise: TouchExercise) {
        _state.value = TouchUiState(exercise = exercise)
        tts.speak(exercise.target)
    }

    fun playTarget() {
        _state.value.exercise?.let { tts.speak(it.target) }
    }

    fun onOptionTapped(index: Int) {
        val exercise = _state.value.exercise ?: return
        if (index in _state.value.foundCorrect) return
        if (_state.value.flashState.containsKey(index)) return

        val isCorrect = index in exercise.correctIndices
        val strings = stringsFor(progressRepository.getSelectedLanguage())

        _state.value = _state.value.copy(
            flashState = _state.value.flashState + (index to if (isCorrect) FlashState.CORRECT else FlashState.INCORRECT)
        )

        viewModelScope.launch {
            tts.speak(if (isCorrect) strings.correct else strings.tryAgain)
            delay(1000)
            _state.value = _state.value.copy(
                flashState = _state.value.flashState - index
            )
            if (isCorrect) {
                val newFound = _state.value.foundCorrect + index
                _state.value = _state.value.copy(
                    foundCorrect = newFound,
                    isCompleted = newFound.containsAll(exercise.correctIndices.toSet())
                )
            }
        }
    }
}
