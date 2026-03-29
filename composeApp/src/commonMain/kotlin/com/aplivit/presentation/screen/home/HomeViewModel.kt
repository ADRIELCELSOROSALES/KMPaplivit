package com.aplivit.presentation.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
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

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val levels = getLevels.execute()
            val progress = progressRepository.loadProgress()
            _state.value = HomeUiState(levels = levels, progress = progress, isLoading = false)
        }
    }

    fun speakWelcome() {
        tts.speak("Bienvenido a Aplivit. Elegí un nivel para comenzar.")
    }

    override fun onCleared() {
        tts.stop()
        super.onCleared()
    }
}
