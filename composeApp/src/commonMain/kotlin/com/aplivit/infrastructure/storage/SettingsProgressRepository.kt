package com.aplivit.infrastructure.storage

import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.port.ProgressRepository
import com.russhwolf.settings.Settings

class SettingsProgressRepository(private val settings: Settings) : ProgressRepository {

    override fun loadProgress(): UserProgress {
        val currentLevel = settings.getInt(KEY_CURRENT_LEVEL, 1)
        val completedRaw = settings.getString(KEY_COMPLETED_LEVELS, "")
        val completedLevels = if (completedRaw.isBlank()) emptySet()
            else completedRaw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        val totalErrors = settings.getInt(KEY_TOTAL_ERRORS, 0)
        return UserProgress(currentLevel, completedLevels, totalErrors)
    }

    override fun saveProgress(progress: UserProgress) {
        settings.putInt(KEY_CURRENT_LEVEL, progress.currentLevel)
        settings.putString(KEY_COMPLETED_LEVELS, progress.completedLevels.joinToString(","))
        settings.putInt(KEY_TOTAL_ERRORS, progress.totalErrors)
    }

    companion object {
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_COMPLETED_LEVELS = "completed_levels"
        private const val KEY_TOTAL_ERRORS = "total_errors"
    }
}
