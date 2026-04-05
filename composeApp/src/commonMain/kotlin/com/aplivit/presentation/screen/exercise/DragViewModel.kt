package com.aplivit.presentation.screen.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.DragExercise
import com.aplivit.core.domain.model.DragType
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Ítem arrastrable con ID único para manejar duplicados (ej: "MA" + "MA" en "MAMA").
 * El id corresponde al índice original del ítem en exercise.items antes del shuffle.
 */
data class DragItem(val id: Int, val text: String)

data class DragUiState(
    val exercise: DragExercise? = null,
    val availableItems: List<DragItem> = emptyList(),
    val slotContents: List<String?> = emptyList(),      // null = slot vacío
    val slotFlash: Map<Int, FlashState> = emptyMap(),
    val isCompleted: Boolean = false
)

class DragViewModel(
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DragUiState())
    val state: StateFlow<DragUiState> = _state

    fun loadExercise(exercise: DragExercise) {
        val shuffled = exercise.items
            .mapIndexed { i, text -> DragItem(id = i, text = text) }
            .shuffled()
        _state.value = DragUiState(
            exercise = exercise,
            availableItems = shuffled,
            slotContents = List(exercise.targetSlots) { null }
        )
        tts.speakWord(buildTargetText(exercise))
    }

    fun playTarget() {
        _state.value.exercise?.let { tts.speakWord(buildTargetText(it)) }
    }

    fun onDropOnSlot(itemId: Int, slotIndex: Int) {
        val exercise = _state.value.exercise ?: return
        if (_state.value.slotContents.getOrNull(slotIndex) != null) return   // slot lleno
        if (_state.value.slotFlash.containsKey(slotIndex)) return            // flash activo

        val item = _state.value.availableItems.firstOrNull { it.id == itemId } ?: return
        val expectedText = exercise.items.getOrNull(slotIndex) ?: return
        val isCorrect = item.text == expectedText

        if (isCorrect) {
            val newContents = _state.value.slotContents.toMutableList().also {
                it[slotIndex] = item.text
            }
            val newAvailable = _state.value.availableItems.filter { it.id != itemId }
            val isNowCompleted = newContents.all { it != null }

            when (exercise.type) {
                DragType.SYLLABLES_TO_WORD -> tts.speakSyllable(item.text)
                DragType.WORDS_TO_SENTENCE -> tts.speakWord(item.text)
            }

            _state.value = _state.value.copy(
                availableItems = newAvailable,
                slotContents = newContents,
                slotFlash = _state.value.slotFlash + (slotIndex to FlashState.CORRECT)
            )

            viewModelScope.launch {
                delay(500)
                _state.value = _state.value.copy(
                    slotFlash = _state.value.slotFlash - slotIndex
                )
                if (isNowCompleted) {
                    delay(200)
                    tts.speakWord(buildTargetText(exercise))
                    _state.value = _state.value.copy(isCompleted = true)
                }
            }
        } else {
            val strings = stringsFor(progressRepository.getSelectedLanguage())
            _state.value = _state.value.copy(
                slotFlash = _state.value.slotFlash + (slotIndex to FlashState.INCORRECT)
            )
            viewModelScope.launch {
                delay(300)
                tts.speak(strings.tryAgain)
                delay(800)
                _state.value = _state.value.copy(
                    slotFlash = _state.value.slotFlash - slotIndex
                )
            }
        }
    }

    private fun buildTargetText(exercise: DragExercise): String =
        exercise.items.joinToString(
            separator = if (exercise.type == DragType.WORDS_TO_SENTENCE) " " else ""
        )
}
