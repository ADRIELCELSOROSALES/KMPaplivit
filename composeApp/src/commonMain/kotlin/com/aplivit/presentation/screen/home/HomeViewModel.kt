package com.aplivit.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.SessionResumeUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.AppStrings
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val levels: List<Level> = emptyList(),
    val progress: UserProgress = UserProgress(),
    val isLoading: Boolean = true,
    val strings: AppStrings = stringsFor(AppLanguage.SPANISH),
    val selectedLanguage: AppLanguage = AppLanguage.SPANISH
)

class HomeViewModel(
    private val getLevels: GetLevelsUseCase,
    private val progressRepository: ProgressRepository,
    private val tts: SpeechSynthesizer,
    private val sessionResume: SessionResumeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private var completionAnnounced = false

    fun reload(completedLevel: Boolean = false) {
        println("TTS_VM [HomeViewModel] reload() completedLevel=$completedLevel")
        viewModelScope.launch {
            val language = progressRepository.getSelectedLanguage()
            val strings = stringsFor(language)
            val levels = getLevels.execute(language)
            val resumeInfo = sessionResume.getResumeInfo()
            val progress = progressRepository.loadProgress(language)

            val safeProgress = if (progress.currentLevel > levels.size && levels.isNotEmpty()) {
                progress.copy(currentLevel = levels.size)
            } else {
                progress
            }

            _state.value = HomeUiState(
                levels = levels,
                progress = safeProgress,
                isLoading = true,
                strings = strings,
                selectedLanguage = language
            )

            println("TTS_VM [HomeViewModel] datos cargados — target nivel=${resumeInfo.targetLevel} ejercicio=${resumeInfo.targetExercise}")
            delay(300)
            tts.setLanguage(language)
            delay(150)

            val message = when {
                resumeInfo.isFirstTime -> {
                    progressRepository.markLaunched()
                    strings.welcome                         // "Bienvenido. Empecemos desde el principio."
                }
                completedLevel && !completionAnnounced -> {
                    completionAnnounced = true
                    strings.nextLevel                       // "Muy bien! Seguimos con el siguiente nivel."
                }
                resumeInfo.hasProgress -> strings.resumeSession  // "Continuamos donde lo dejaste."
                else -> strings.selectLevel
            }

            println("TTS_VM [HomeViewModel] hablando: '$message'")
            tts.speakAndWait(message)
            println("TTS_VM [HomeViewModel] speakAndWait COMPLETÓ")

            // Navegación DESPUÉS de que el TTS termina, para no interrumpir la instrucción del nivel
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    override fun onCleared() {
        println("TTS_VM [HomeViewModel] onCleared() — NO llama tts.stop()")
        super.onCleared()
    }
}
