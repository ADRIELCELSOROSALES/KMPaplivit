package com.aplivit.presentation.screen.recap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.AppStrings
import com.aplivit.shared.stringsFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RecapUiState(
    val syllables: List<String> = emptyList(),
    val strings: AppStrings? = null,
    val isLoading: Boolean = true
)

class RecapViewModel(
    private val getLevels: GetLevelsUseCase,
    private val progressRepository: ProgressRepository,
    private val tts: SpeechSynthesizer
) : ViewModel() {

    private val _state = MutableStateFlow(RecapUiState())
    val state: StateFlow<RecapUiState> = _state

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val language = progressRepository.getSelectedLanguage()
            val strings = stringsFor(language)
            val progress = progressRepository.loadProgress(language)
            val allLevels = getLevels.execute(language)

            val syllables = allLevels
                .filter { it.id in progress.completedLevels }
                .sortedBy { it.id }
                .flatMap { level -> level.syllables.map { it.text } }
                .distinct()

            _state.value = RecapUiState(syllables = syllables, strings = strings, isLoading = false)

            tts.setLanguage(language)
            speakInstruction(strings)
        }
    }

    fun speakInstruction() {
        val strings = _state.value.strings ?: return
        speakInstruction(strings)
    }

    private fun speakInstruction(strings: AppStrings) {
        tts.speak("${strings.recapTitle}. ${strings.tapSyllableHint}")
    }

    fun speakSyllable(text: String) {
        tts.speakSyllable(text)
    }
}
