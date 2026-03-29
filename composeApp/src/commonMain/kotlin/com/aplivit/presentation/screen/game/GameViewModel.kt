package com.aplivit.presentation.screen.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.usecase.CompleteGameUseCase
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.UnlockNextLevelUseCase
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class GameStep { DRAG_DROP, SELECTION, REPEAT, COMPLETED }

data class GameUiState(
    val level: Level? = null,
    val currentStep: GameStep = GameStep.DRAG_DROP,
    val isLoading: Boolean = true,
    val feedback: String? = null,
    val errors: Int = 0,
    val recognitionMode: RecognitionMode = RecognitionMode.AMPLITUDE,
    val isListening: Boolean = false,
    val availableSyllables: List<String> = emptyList(),
    val arrangedSyllables: List<String> = emptyList()
)

class GameViewModel(
    private val levelId: Int,
    private val getLevels: GetLevelsUseCase,
    private val completeGame: CompleteGameUseCase,
    private val unlockNext: UnlockNextLevelUseCase,
    private val validatePronunciation: ValidatePronunciationUseCase,
    private val recognizer: SpeechRecognizer,
    private val tts: SpeechSynthesizer
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val level = getLevels.execute().find { it.id == levelId }
            _state.value = GameUiState(
                level = level,
                isLoading = false,
                recognitionMode = recognizer.mode,
                availableSyllables = level?.syllables?.map { it.text }?.shuffled() ?: emptyList()
            )
        }
    }

    fun onSyllableMoved(syllable: String) {
        _state.value = _state.value.copy(
            availableSyllables = _state.value.availableSyllables - syllable,
            arrangedSyllables = _state.value.arrangedSyllables + syllable
        )
    }

    fun onDragDropReset() {
        val all = _state.value.arrangedSyllables + _state.value.availableSyllables
        _state.value = _state.value.copy(
            availableSyllables = all,
            arrangedSyllables = emptyList()
        )
    }

    fun onDragDropCompleted(correct: Boolean) {
        if (correct) {
            tts.speak("Muy bien. Ahora escucha y elige la sílaba correcta.")
            _state.value = _state.value.copy(currentStep = GameStep.SELECTION, feedback = null)
        } else {
            val errors = _state.value.errors + 1
            tts.speak("Inténtalo de nuevo.")
            _state.value = _state.value.copy(errors = errors, feedback = "Inténtalo de nuevo")
        }
    }

    fun onSelectionCompleted(correct: Boolean) {
        if (correct) {
            tts.speak("Excelente. Ahora repite lo que escuchas.")
            _state.value = _state.value.copy(
                currentStep = GameStep.REPEAT,
                feedback = null,
                recognitionMode = recognizer.mode
            )
        } else {
            val errors = _state.value.errors + 1
            tts.speak("Eso no es correcto. Inténtalo de nuevo.")
            _state.value = _state.value.copy(errors = errors, feedback = "Inténtalo de nuevo")
        }
    }

    fun startListening(expected: String) {
        _state.value = _state.value.copy(isListening = true)
        recognizer.startListening(expected) { result ->
            viewModelScope.launch {
                handleRecognitionResult(result, expected)
            }
        }
    }

    fun stopListening() {
        recognizer.stopListening()
        _state.value = _state.value.copy(isListening = false)
    }

    private suspend fun handleRecognitionResult(result: RecognitionResult, expected: String) {
        _state.value = _state.value.copy(isListening = false)
        when (result) {
            is RecognitionResult.Transcription -> {
                val correct = validatePronunciation.execute(result, expected)
                if (correct) onRepeatSuccess() else onRepeatError()
            }
            is RecognitionResult.SoundDetected -> onRepeatSuccess()
            is RecognitionResult.NoSound -> {
                tts.speak("No te escuché. Inténtalo de nuevo.")
                _state.value = _state.value.copy(errors = _state.value.errors + 1, feedback = "No te escuché")
            }
            is RecognitionResult.Error -> {
                tts.speak("Hubo un error. Inténtalo de nuevo.")
                _state.value = _state.value.copy(errors = _state.value.errors + 1, feedback = "Error al reconocer")
            }
            is RecognitionResult.PermissionDenied -> {
                tts.speak("Se necesita permiso de micrófono para este ejercicio.")
                _state.value = _state.value.copy(feedback = "Se necesita permiso de micrófono")
            }
        }
    }

    private suspend fun onRepeatSuccess() {
        completeGame.execute(levelId, _state.value.errors)
        unlockNext.execute(levelId)
        // Hablar ANTES de cambiar el estado para que la navegación ocurra
        // solo después de que el audio haya terminado completamente
        tts.speakAndWait("¡Muy bien! Completaste el nivel.")
        _state.value = _state.value.copy(currentStep = GameStep.COMPLETED, feedback = null)
    }

    private fun onRepeatError() {
        tts.speak("No fue correcto. Inténtalo de nuevo.")
        _state.value = _state.value.copy(errors = _state.value.errors + 1, feedback = "Inténtalo de nuevo")
    }

    override fun onCleared() {
        println("TTS_VM [GameViewModel] onCleared() levelId=$levelId — NO llama tts.stop()")
        recognizer.stopListening()
        super.onCleared()
    }
}
