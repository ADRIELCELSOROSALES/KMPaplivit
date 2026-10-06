package com.aplivit.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.port.ContentRepository
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedLanguage: AppLanguage = AppLanguage.ENGLISH
)

class SettingsViewModel(
    private val progressRepository: ProgressRepository,
    private val tts: SpeechSynthesizer,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        _state.value = SettingsUiState(selectedLanguage = progressRepository.getSelectedLanguage())
    }

    fun selectLanguage(language: AppLanguage) {
        progressRepository.saveSelectedLanguage(language)
        _state.value = _state.value.copy(selectedLanguage = language)
        viewModelScope.launch {
            tts.setLanguage(language)
            // Sincroniza el idioma con el backend y re-baja el contenido en el idioma nuevo.
            runCatching { contentRepository.refreshContent() }
        }
    }
}
