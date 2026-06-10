package com.aplivit.presentation.screen.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.LinkExercise
import com.aplivit.core.domain.model.LinkPair
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LinkUiState(
    val exercise: LinkExercise? = null,
    val selectedLeft: Int? = null,
    val confirmedPairs: Map<Int, Int> = emptyMap(),  // left index -> right index
    val flashPair: LinkPair? = null,
    val flashCorrect: Boolean = false,
    val isCompleted: Boolean = false
)

class LinkViewModel(
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LinkUiState())
    val state: StateFlow<LinkUiState> = _state

    fun loadExercise(exercise: LinkExercise) {
        _state.value = LinkUiState(exercise = exercise)
        exercise.leftItems.firstOrNull()?.text?.let { tts.speakWord(it) }
    }

    fun playTarget() {
        // Reproduce todos los ítems de la izquierda en secuencia
        val exercise = _state.value.exercise ?: return
        viewModelScope.launch {
            exercise.leftItems.forEach { item ->
                item.text?.let { tts.speakWord(it) }
                delay(600)
            }
        }
    }

    fun onLeftTapped(index: Int) {
        val exercise = _state.value.exercise ?: return
        if (index in _state.value.confirmedPairs) return
        if (_state.value.flashPair != null) return

        // Tap sobre el ítem ya seleccionado → deselecciona; otro → cambia selección
        val newSelected = if (_state.value.selectedLeft == index) null else index
        _state.value = _state.value.copy(selectedLeft = newSelected)

        if (newSelected != null) {
            exercise.leftItems[index].text?.let { tts.speakWord(it) }
        }
    }

    fun onRightTapped(index: Int) {
        val left = _state.value.selectedLeft ?: return
        val exercise = _state.value.exercise ?: return
        if (_state.value.flashPair != null) return
        if (_state.value.confirmedPairs.values.contains(index)) return

        val isCorrect = exercise.correctPairs.any { it.left == left && it.right == index }
        val strings = stringsFor(progressRepository.getSelectedLanguage())

        // Feedback de voz: primero el ítem derecho, luego correcto/incorrecto
        exercise.rightItems[index].text?.let { tts.speakWord(it) }

        _state.value = _state.value.copy(
            selectedLeft = null,
            flashPair = LinkPair(left, index),
            flashCorrect = isCorrect
        )

        viewModelScope.launch {
            delay(400)
            tts.speak(if (isCorrect) strings.correct else strings.tryAgain)
            delay(600)
            if (isCorrect) {
                val newPairs = _state.value.confirmedPairs + (left to index)
                val isNowCompleted = newPairs.size == exercise.correctPairs.size
                _state.value = _state.value.copy(
                    confirmedPairs = newPairs,
                    flashPair = null,
                    isCompleted = isNowCompleted
                )
            } else {
                _state.value = _state.value.copy(flashPair = null)
            }
        }
    }
}
