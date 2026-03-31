package com.aplivit.presentation.screen.level

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.AppStrings
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LevelUiState(
    val level: Level? = null,
    val isLoading: Boolean = true,
    val strings: AppStrings = stringsFor(AppLanguage.SPANISH)
)

class LevelViewModel(
    private val levelId: Int,
    private val getLevels: GetLevelsUseCase,
    private val tts: SpeechSynthesizer,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LevelUiState())
    val state: StateFlow<LevelUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val language = progressRepository.getSelectedLanguage()
            val strings = stringsFor(language)
            val level = getLevels.execute(language).find { it.id == levelId }
            _state.value = LevelUiState(level = level, isLoading = false, strings = strings)
            tts.setLanguage(language)
            delay(150)
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
