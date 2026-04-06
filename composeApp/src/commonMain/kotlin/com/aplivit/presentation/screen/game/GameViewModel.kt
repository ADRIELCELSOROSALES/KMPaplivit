package com.aplivit.presentation.screen.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.usecase.CompleteGameUseCase
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.UnlockNextLevelUseCase
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.AppStrings
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
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
    val arrangedSyllables: List<String> = emptyList(),
    val strings: AppStrings = stringsFor(AppLanguage.SPANISH)
)

class GameViewModel(
    private val levelId: Int,
    private val getLevels: GetLevelsUseCase,
    private val completeGame: CompleteGameUseCase,
    private val unlockNext: UnlockNextLevelUseCase,
    private val validatePronunciation: ValidatePronunciationUseCase,
    private val recognizer: SpeechRecognizer,
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val language = progressRepository.getSelectedLanguage()
            val strings = stringsFor(language)
            val level = getLevels.execute(language).find { it.id == levelId }
            _state.value = GameUiState(
                level = level,
                isLoading = false,
                recognitionMode = recognizer.mode,
                availableSyllables = level?.syllables?.map { it.text }?.shuffled() ?: emptyList(),
                strings = strings
            )
            tts.setLanguage(language)
            level?.let { tts.speak("${it.instruction} ${strings.dragDropInstruction}") }

            // Guardar posición actual para retomar sesión al volver a abrir la app
            val progress = progressRepository.loadProgress(language)
            progressRepository.saveProgress(
                progress.copy(currentLevel = levelId, currentExercise = 1),
                language
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
        val strings = _state.value.strings
        if (correct) {
            viewModelScope.launch {
                tts.speakAndWait(strings.dragDropSuccess)
                _state.value = _state.value.copy(currentStep = GameStep.SELECTION, feedback = null)
            }
        } else {
            val errors = _state.value.errors + 1
            tts.speak(strings.tryAgain)
            _state.value = _state.value.copy(errors = errors, feedback = strings.tryAgain)
        }
    }

    fun onSelectionCompleted(correct: Boolean) {
        val strings = _state.value.strings
        if (correct) {
            viewModelScope.launch {
                tts.speakAndWait(strings.selectionSuccess)
                _state.value = _state.value.copy(
                    currentStep = GameStep.REPEAT,
                    feedback = null,
                    recognitionMode = recognizer.mode
                )
            }
        } else {
            val errors = _state.value.errors + 1
            tts.speak(strings.selectionError)
            _state.value = _state.value.copy(errors = errors, feedback = strings.tryAgain)
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
        val strings = _state.value.strings
        when (result) {
            is RecognitionResult.Transcription -> {
                val correct = validatePronunciation.execute(result, expected)
                if (correct) onRepeatSuccess() else onRepeatError()
            }
            is RecognitionResult.SoundDetected -> onRepeatSuccess()
            is RecognitionResult.NoSound -> {
                tts.speak(strings.noSoundDetected)
                _state.value = _state.value.copy(errors = _state.value.errors + 1, feedback = strings.noSoundDetected)
            }
            is RecognitionResult.Error -> {
                tts.speak(strings.recognitionError)
                _state.value = _state.value.copy(errors = _state.value.errors + 1, feedback = strings.recognitionError)
            }
            is RecognitionResult.PermissionDenied -> {
                tts.speak(strings.permissionNeeded)
                _state.value = _state.value.copy(feedback = strings.permissionNeeded)
            }
        }
    }

    private suspend fun onRepeatSuccess() {
        completeGame.execute(levelId, _state.value.errors)
        unlockNext.execute(levelId)
        tts.speakAndWait(_state.value.strings.levelCompleted)
        _state.value = _state.value.copy(currentStep = GameStep.COMPLETED, feedback = null)
    }

    private fun onRepeatError() {
        val strings = _state.value.strings
        tts.speak(strings.repeatError)
        _state.value = _state.value.copy(errors = _state.value.errors + 1, feedback = strings.tryAgain)
    }

    override fun onCleared() {
        println("TTS_VM [GameViewModel] onCleared() levelId=$levelId — NO llama tts.stop()")
        recognizer.stopListening()
        super.onCleared()
    }
}
