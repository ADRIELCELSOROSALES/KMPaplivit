package com.aplivit.presentation.screen.exercise

import androidx.lifecycle.ViewModel
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LetterTracingUiState(
    val letter: String = "",
    val isAnimating: Boolean = false,
    val isCompleted: Boolean = false,
    /** Incremented each time the animation should (re)start. */
    val playKey: Int = 0
)

class LetterTracingViewModel(
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LetterTracingUiState())
    val state: StateFlow<LetterTracingUiState> = _state

    /** Call once when the screen opens with the target letter/syllable. */
    fun loadLetter(letter: String) {
        _state.value = LetterTracingUiState(letter = letter)
        speakLetter(letter)
        triggerAnimation()
    }

    /** Called by the ear button — replays TTS pronunciation only. */
    fun replay() {
        speakLetter(_state.value.letter)
    }

    /** Called by the composable when the animation finishes all strokes. */
    fun onAnimationComplete() {
        _state.value = _state.value.copy(isAnimating = false, isCompleted = true)
    }

    private fun triggerAnimation() {
        _state.value = _state.value.copy(
            isAnimating = true,
            isCompleted = false,
            playKey = _state.value.playKey + 1
        )
    }

    private fun speakLetter(letter: String) {
        if (letter.isNotBlank()) tts.speakSyllable(letter)
    }
}
