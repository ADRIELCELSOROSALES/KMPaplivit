package com.aplivit.presentation.screen.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LevelUiState(
    val level: Level? = null,
    val isLoading: Boolean = true
)

class LevelViewModel(
    private val levelId: Int,
    private val getLevels: GetLevelsUseCase,
    private val tts: SpeechSynthesizer
) : ViewModel() {

    private val _state = MutableStateFlow(LevelUiState())
    val state: StateFlow<LevelUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val level = getLevels.execute().find { it.id == levelId }
            _state.value = LevelUiState(level = level, isLoading = false)
            level?.let { l -> tts.speak(l.instruction) }
        }
    }

    fun speakSyllable(syllable: String) {
        tts.speak(syllable)
    }

    override fun onCleared() {
        println("TTS_VM [LevelViewModel] onCleared() levelId=$levelId — NO llama tts.stop()")
        super.onCleared()
    }
}
