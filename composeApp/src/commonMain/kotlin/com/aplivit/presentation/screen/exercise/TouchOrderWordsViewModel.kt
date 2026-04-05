package com.aplivit.presentation.screen.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.SentenceExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TouchOrderWordsUiState(
    val exercise: SentenceExercise? = null,
    // correctTapOrder[paso] = qué posición de display debe tocarse en el paso N
    val correctTapOrder: List<Int> = emptyList(),
    // posiciones de display tocadas correctamente en orden
    val selectedOrder: List<Int> = emptyList(),
    val flashStates: Map<Int, FlashState> = emptyMap(),
    val isCompleted: Boolean = false
)

class TouchOrderWordsViewModel(
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TouchOrderWordsUiState())
    val state: StateFlow<TouchOrderWordsUiState> = _state

    fun loadExercise(exercise: SentenceExercise) {
        // Para cada palabra en orden correcto (índice 0,1,2...), buscar en qué posición
        // de display aparece, es decir: shuffledIndices.indexOf(wordIdx)
        val correctTapOrder = exercise.words.indices.map { wordIdx ->
            exercise.shuffledIndices.indexOf(wordIdx)
        }
        _state.value = TouchOrderWordsUiState(
            exercise = exercise,
            correctTapOrder = correctTapOrder
        )
        tts.speakSentence(exercise.words.joinToString(" "))
    }

    fun playTarget() {
        val exercise = _state.value.exercise ?: return
        tts.speakSentence(exercise.words.joinToString(" "))
    }

    fun onWordTapped(displayIndex: Int) {
        val exercise = _state.value.exercise ?: return
        if (displayIndex in _state.value.selectedOrder) return
        if (_state.value.flashStates.containsKey(displayIndex)) return
        if (_state.value.isCompleted) return

        val expectedDisplayIndex = _state.value.correctTapOrder.getOrNull(_state.value.selectedOrder.size) ?: return
        val isCorrect = displayIndex == expectedDisplayIndex

        // Pronunciar la palabra tocada
        val wordIndex = exercise.shuffledIndices[displayIndex]
        tts.speakWord(exercise.words[wordIndex])

        if (isCorrect) {
            val newSelected = _state.value.selectedOrder + displayIndex
            val isNowCompleted = newSelected.size == exercise.words.size

            _state.value = _state.value.copy(
                selectedOrder = newSelected,
                flashStates = _state.value.flashStates + (displayIndex to FlashState.CORRECT)
            )

            viewModelScope.launch {
                delay(500)
                _state.value = _state.value.copy(
                    flashStates = _state.value.flashStates - displayIndex
                )
                if (isNowCompleted) {
                    delay(200)
                    tts.speakSentence(exercise.words.joinToString(" "))
                    _state.value = _state.value.copy(isCompleted = true)
                }
            }
        } else {
            val strings = stringsFor(progressRepository.getSelectedLanguage())
            _state.value = _state.value.copy(
                flashStates = _state.value.flashStates + (displayIndex to FlashState.INCORRECT)
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
