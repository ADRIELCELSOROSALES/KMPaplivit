package com.aplivit.presentation.screen.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.GameResult
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.domain.usecase.CompleteGameUseCase
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.UnlockNextLevelUseCase
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.core.port.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class GameStep { DRAG_DROP, SELECTION, REPEAT, COMPLETED }

data class GameUiState(
    val level: Level? = null,
    val progress: UserProgress = UserProgress(),
    val currentStep: GameStep = GameStep.DRAG_DROP,
    val isLoading: Boolean = true,
    val feedback: String = "",
    val errors: Int = 0,
    val recognitionMode: RecognitionMode = RecognitionMode.AMPLITUDE,
    val isListening: Boolean = false
)

class GameViewModel(
    private val levelId: Int,
    private val getLevels: GetLevelsUseCase,
    private val completeGame: CompleteGameUseCase,
    private val unlockNext: UnlockNextLevelUseCase,
    private val validatePronunciation: ValidatePronunciationUseCase,
    private val tts: SpeechSynthesizer,
    private val recognizer: SpeechRecognizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val level = getLevels().find { it.id == levelId }
            val progress = progressRepository.loadProgress()
            _state.value = GameUiState(
                level = level,
                progress = progress,
                isLoading = false,
                recognitionMode = recognizer.mode
            )
        }
    }

    fun onDragDropCompleted(correct: Boolean) {
        if (correct) {
            tts.speak("Muy bien. Ahora escucha y elige la sílaba correcta.")
            _state.value = _state.value.copy(currentStep = GameStep.SELECTION, feedback = "")
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
                feedback = "",
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

    private fun handleRecognitionResult(result: RecognitionResult, expected: String) {
        _state.value = _state.value.copy(isListening = false)
        when (result) {
            is RecognitionResult.Transcription -> {
                val correct = validatePronunciation(result.text, expected)
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
        }
    }

    private fun onRepeatSuccess() {
        tts.speak("Muy bien. Completaste el nivel.")
        val currentProgress = _state.value.progress
        val errors = _state.value.errors
        val updatedProgress = completeGame(currentProgress, levelId, errors)
        val finalProgress = unlockNext(updatedProgress, levelId)
        _state.value = _state.value.copy(
            currentStep = GameStep.COMPLETED,
            progress = finalProgress,
            feedback = ""
        )
    }

    private fun onRepeatError() {
        tts.speak("No fue correcto. Inténtalo de nuevo.")
        _state.value = _state.value.copy(errors = _state.value.errors + 1, feedback = "Inténtalo de nuevo")
    }

    override fun onCleared() {
        recognizer.stopListening()
        tts.stop()
        super.onCleared()
    }
}
