package com.aplivit.infrastructure.storage

import com.aplivit.config.AppConfig
import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.port.ProgressRepository
import com.russhwolf.settings.Settings

class SettingsProgressRepository(private val settings: Settings) : ProgressRepository {

    // ── ProgressRepository ────────────────────────────────────────────────

    override fun loadProgress(): UserProgress = loadProgress(getSelectedLanguage())

    override fun saveProgress(progress: UserProgress) = saveProgress(progress, getSelectedLanguage())

    override fun loadProgress(language: AppLanguage): UserProgress {
        val prefix = language.code
        val currentLevel = settings.getInt("${prefix}_$KEY_CURRENT_LEVEL", 1)
        val completedRaw = settings.getString("${prefix}_$KEY_COMPLETED_LEVELS", "")
        val completedLevels = if (completedRaw.isBlank()) emptySet()
            else completedRaw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        val totalErrors = settings.getInt("${prefix}_$KEY_TOTAL_ERRORS", 0)

        // maxUnlockedLevel defaults to currentLevel so existing users keep their progress
        val maxUnlockedLevel = settings.getInt(
            "${prefix}_$KEY_MAX_UNLOCKED_LEVEL",
            defaultValue = currentLevel
        )
        val maxUnlockedExercise = settings.getInt("${prefix}_$KEY_MAX_UNLOCKED_EXERCISE", 1)
        val currentExercise = settings.getInt("${prefix}_$KEY_CURRENT_EXERCISE", 1)

        return UserProgress(
            currentLevel = currentLevel,
            currentExercise = currentExercise,
            maxUnlockedLevel = maxUnlockedLevel,
            maxUnlockedExercise = maxUnlockedExercise,
            completedLevels = completedLevels,
            totalErrors = totalErrors
        )
    }

    override fun saveProgress(progress: UserProgress, language: AppLanguage) {
        val prefix = language.code
        settings.putInt("${prefix}_$KEY_CURRENT_LEVEL", progress.currentLevel)
        settings.putInt("${prefix}_$KEY_CURRENT_EXERCISE", progress.currentExercise)
        settings.putInt("${prefix}_$KEY_MAX_UNLOCKED_LEVEL", progress.maxUnlockedLevel)
        settings.putInt("${prefix}_$KEY_MAX_UNLOCKED_EXERCISE", progress.maxUnlockedExercise)
        settings.putString(
            "${prefix}_$KEY_COMPLETED_LEVELS",
            progress.completedLevels.joinToString(",")
        )
        settings.putInt("${prefix}_$KEY_TOTAL_ERRORS", progress.totalErrors)
    }

    override fun getSelectedLanguage(): AppLanguage {
        val default = AppLanguage.fromCode(AppConfig.DEFAULT_LANGUAGE_CODE)
        val code = settings.getString(KEY_SELECTED_LANGUAGE, default.code)
        return AppLanguage.entries.find { it.code == code } ?: default
    }

    override fun saveSelectedLanguage(language: AppLanguage) {
        settings.putString(KEY_SELECTED_LANGUAGE, language.code)
    }

    // ── First-launch flag (outside UserProgress) ──────────────────────────

    override fun isFirstLaunch(): Boolean = settings.getBoolean(KEY_IS_FIRST_LAUNCH, true)

    override fun markLaunched() = settings.putBoolean(KEY_IS_FIRST_LAUNCH, false)

    // ── Reset del espejo local (cambio de cuenta) ─────────────────────────

    override fun resetProgress() {
        AppLanguage.entries.forEach { language ->
            val prefix = language.code
            settings.remove("${prefix}_$KEY_CURRENT_LEVEL")
            settings.remove("${prefix}_$KEY_CURRENT_EXERCISE")
            settings.remove("${prefix}_$KEY_MAX_UNLOCKED_LEVEL")
            settings.remove("${prefix}_$KEY_MAX_UNLOCKED_EXERCISE")
            settings.remove("${prefix}_$KEY_COMPLETED_LEVELS")
            settings.remove("${prefix}_$KEY_TOTAL_ERRORS")
        }
    }

    // ── Keys ──────────────────────────────────────────────────────────────

    companion object {
        private const val KEY_CURRENT_LEVEL = "current_level"
        private const val KEY_CURRENT_EXERCISE = "current_exercise"
        private const val KEY_MAX_UNLOCKED_LEVEL = "max_unlocked_level"
        private const val KEY_MAX_UNLOCKED_EXERCISE = "max_unlocked_exercise"
        private const val KEY_COMPLETED_LEVELS = "completed_levels"
        private const val KEY_TOTAL_ERRORS = "total_errors"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_SELECTED_LANGUAGE = "selected_language"
    }
}
