package com.aplivit.presentation.screen.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.VocalizeExercise
import com.aplivit.core.domain.model.VocalizeType
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class VocalizeFeedback { CORRECT, INCORRECT }

data class VocalizeUiState(
    val exercise: VocalizeExercise? = null,
    val isListening: Boolean = false,
    val feedback: VocalizeFeedback? = null,
    val feedbackMessage: String = "",
    val isCompleted: Boolean = false,
    /** true = STT disponible; false = solo detección de amplitud */
    val useStrict: Boolean = true
)

class VocalizeViewModel(
    private val tts: SpeechSynthesizer,
    private val recognizer: SpeechRecognizer,
    private val connectivity: ConnectivityChecker,
    private val validatePronunciation: ValidatePronunciationUseCase,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VocalizeUiState())
    val state: StateFlow<VocalizeUiState> = _state

    fun loadExercise(exercise: VocalizeExercise) {
        val useStrict = connectivity.isConnected() && recognizer.mode == RecognitionMode.STT
        _state.value = VocalizeUiState(exercise = exercise, useStrict = useStrict)
        playContent(exercise)
    }

    fun onListenClick() {
        _state.value.exercise?.let { playContent(it) }
    }

    fun toggleListening() {
        if (_state.value.isListening) stopListening()
        else startListening()
    }

    private fun startListening() {
        val exercise = _state.value.exercise ?: return
        if (_state.value.isCompleted) return
        _state.value = _state.value.copy(isListening = true, feedback = null, feedbackMessage = "")
        recognizer.startListening(exercise.content) { result ->
            viewModelScope.launch { handleResult(result) }
        }
    }

    fun stopListening() {
        recognizer.stopListening()
        _state.value = _state.value.copy(isListening = false)
    }

    private suspend fun handleResult(result: RecognitionResult) {
        _state.value = _state.value.copy(isListening = false)
        val exercise = _state.value.exercise ?: return
        val strings = stringsFor(progressRepository.getSelectedLanguage())

        val isCorrect = when (result) {
            is RecognitionResult.Transcription -> {
                // Modo estricto: STT con conexión — validar transcripción
                // Modo permisivo: sin conexión — cualquier transcripción cuenta
                if (_state.value.useStrict) validatePronunciation.execute(result, exercise.content)
                else true
            }
            is RecognitionResult.SoundDetected -> true   // modo amplitud
            is RecognitionResult.NoSound -> {
                showFeedback(VocalizeFeedback.INCORRECT, strings.noSoundDetected)
                return
            }
            is RecognitionResult.Error -> {
                showFeedback(VocalizeFeedback.INCORRECT, strings.recognitionError)
                return
            }
            is RecognitionResult.PermissionDenied -> {
                showFeedback(VocalizeFeedback.INCORRECT, strings.permissionNeeded)
                return
            }
        }

        if (isCorrect) {
            tts.speak(strings.correct)
            _state.value = _state.value.copy(
                feedback = VocalizeFeedback.CORRECT,
                feedbackMessage = strings.correct
            )
            delay(1000)
            _state.value = _state.value.copy(isCompleted = true, feedback = null)
        } else {
            showFeedback(VocalizeFeedback.INCORRECT, strings.repeatError)
        }
    }

    private suspend fun showFeedback(type: VocalizeFeedback, message: String) {
        if (type == VocalizeFeedback.INCORRECT) tts.speak(message)
        _state.value = _state.value.copy(feedback = type, feedbackMessage = message)
        delay(1200)
        _state.value = _state.value.copy(feedback = null, feedbackMessage = "")
    }

    private fun playContent(exercise: VocalizeExercise) {
        when (exercise.type) {
            VocalizeType.SYLLABLE -> tts.speakSyllable(exercise.content)
            VocalizeType.WORD -> tts.speakWord(exercise.content)
            VocalizeType.SENTENCE -> tts.speakSentence(exercise.content)
        }
    }

    override fun onCleared() {
        recognizer.stopListening()
        super.onCleared()
    }
}
