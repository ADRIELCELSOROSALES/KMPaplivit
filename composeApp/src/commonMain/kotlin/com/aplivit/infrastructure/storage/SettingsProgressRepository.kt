package com.aplivit.infrastructure.storage

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.port.ProgressRepository
import com.russhwolf.settings.Settings

class SettingsProgressRepository(private val settings: Settings) : ProgressRepository {

    override fun loadProgress(): UserProgress = loadProgress(getSelectedLanguage())

    override fun saveProgress(progress: UserProgress) = saveProgress(progress, getSelectedLanguage())

    override fun loadProgress(language: AppLanguage): UserProgress {
        val prefix = language.code
        val currentLevel = settings.getInt("${prefix}_$KEY_CURRENT_LEVEL", 1)
        val completedRaw = settings.getString("${prefix}_$KEY_COMPLETED_LEVELS", "")
        val completedLevels = if (completedRaw.isBlank()) emptySet()
            else completedRaw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        val totalErrors = settings.getInt("${prefix}_$KEY_TOTAL_ERRORS", 0)
        val isFirstLaunch = settings.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        return UserProgress(currentLevel, completedLevels, totalErrors, isFirstLaunch)
    }

    override fun saveProgress(progress: UserProgress, language: AppLanguage) {
        val prefix = language.code
        settings.putInt("${prefix}_$KEY_CURRENT_LEVEL", progress.currentLevel)
        settings.putString("${prefix}_$KEY_COMPLETED_LEVELS", progress.completedLevels.joinToString(","))
        settings.putInt("${prefix}_$KEY_TOTAL_ERRORS", progress.totalErrors)
        settings.putBoolean(KEY_IS_FIRST_LAUNCH, progress.isFirstLaunch)
    }

    override fun getSelectedLanguage(): AppLanguage {
        val code = settings.getString(KEY_SELECTED_LANGUAGE, AppLanguage.SPANISH.code)
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.SPANISH
    }

    override fun saveSelectedLanguage(language: AppLanguage) {
        settings.putString(KEY_SELECTED_LANGUAGE, language.code)
    }

    companion object {
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_COMPLETED_LEVELS = "completed_levels"
        private const val KEY_TOTAL_ERRORS = "total_errors"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_SELECTED_LANGUAGE = "selected_language"
    }
}
