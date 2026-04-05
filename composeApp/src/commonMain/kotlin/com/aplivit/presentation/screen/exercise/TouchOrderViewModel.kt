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

data class TouchOrderUiState(
    val exercise: TouchExercise? = null,
    val selectedOrder: List<Int> = emptyList(),  // indices tocados en orden correcto hasta ahora
    val flashStates: Map<Int, FlashState> = emptyMap(),
    val isCompleted: Boolean = false
)

class TouchOrderViewModel(
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TouchOrderUiState())
    val state: StateFlow<TouchOrderUiState> = _state

    fun loadExercise(exercise: TouchExercise) {
        _state.value = TouchOrderUiState(exercise = exercise)
        tts.speakWord(exercise.target)
    }

    fun playTarget() {
        _state.value.exercise?.let { tts.speakWord(it.target) }
    }

    fun onSyllableTapped(index: Int) {
        val exercise = _state.value.exercise ?: return
        if (index in _state.value.selectedOrder) return
        if (_state.value.flashStates.containsKey(index)) return
        if (_state.value.isCompleted) return

        val expectedIndex = exercise.correctIndices.getOrNull(_state.value.selectedOrder.size) ?: return
        val isCorrect = index == expectedIndex

        tts.speakSyllable(exercise.options[index])

        if (isCorrect) {
            val newSelected = _state.value.selectedOrder + index
            val isNowCompleted = newSelected.size == exercise.correctIndices.size

            _state.value = _state.value.copy(
                selectedOrder = newSelected,
                flashStates = _state.value.flashStates + (index to FlashState.CORRECT)
            )

            viewModelScope.launch {
                delay(500)
                _state.value = _state.value.copy(
                    flashStates = _state.value.flashStates - index
                )
                if (isNowCompleted) {
                    delay(200)
                    tts.speakWord(exercise.target)
                    _state.value = _state.value.copy(isCompleted = true)
                }
            }
        } else {
            val strings = stringsFor(progressRepository.getSelectedLanguage())
            _state.value = _state.value.copy(
                flashStates = _state.value.flashStates + (index to FlashState.INCORRECT)
            )
            viewModelScope.launch {
                delay(600)
                tts.speak(strings.tryAgain)
                delay(900)
                _state.value = _state.value.copy(
                    selectedOrder = emptyList(),
                    flashStates = emptyMap()
                )
            }
        }
    }
}
