package com.aplivit.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val levels: List<Level> = emptyList(),
    val progress: UserProgress = UserProgress(),
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val getLevels: GetLevelsUseCase,
    private val progressRepository: ProgressRepository,
    private val tts: SpeechSynthesizer
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private var completionAnnounced = false

    fun reload(completedLevel: Boolean = false) {
        println("TTS_VM [HomeViewModel] reload() completedLevel=$completedLevel")
        viewModelScope.launch {
            val levels = if (_state.value.levels.isEmpty()) getLevels.execute() else _state.value.levels
            val progress = progressRepository.loadProgress()
            _state.value = _state.value.copy(levels = levels, progress = progress, isLoading = false)
            println("TTS_VM [HomeViewModel] reload() datos cargados, esperando 300ms antes de hablar")
            delay(300)
            println("TTS_VM [HomeViewModel] reload() delay terminado, isFirstLaunch=${progress.isFirstLaunch} completedLevel=$completedLevel completionAnnounced=$completionAnnounced")
            when {
                progress.isFirstLaunch -> {
                    progressRepository.saveProgress(progress.copy(isFirstLaunch = false))
                    speakWelcome()
                }
                completedLevel && !completionAnnounced -> {
                    completionAnnounced = true
                    println("TTS_VM [HomeViewModel] hablando: muy bien selecciona siguiente")
                    tts.speakAndWait("¡Muy bien! Seleccioná el siguiente nivel para continuar.")
                    println("TTS_VM [HomeViewModel] speakAndWait COMPLETÓ: muy bien selecciona siguiente")
                }
                else -> {
                    println("TTS_VM [HomeViewModel] hablando: selecciona un nivel")
                    tts.speakAndWait("Seleccioná un nivel para continuar.")
                    println("TTS_VM [HomeViewModel] speakAndWait COMPLETÓ: selecciona un nivel")
                }
            }
        }
    }

    private suspend fun speakWelcome() {
        println("TTS_VM [HomeViewModel] hablando: bienvenido")
        tts.speakAndWait("Bienvenido a Aplivit. Elegí un nivel para comenzar.")
        println("TTS_VM [HomeViewModel] speakAndWait COMPLETÓ: bienvenido")
    }

    override fun onCleared() {
        println("TTS_VM [HomeViewModel] onCleared() — NO llama tts.stop()")
        super.onCleared()
    }
}
