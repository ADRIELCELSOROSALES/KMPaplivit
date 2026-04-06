package com.aplivit.core.domain.usecase

import com.aplivit.core.port.ProgressRepository

/**
 * Determina el estado de la sesión al iniciar la app.
 *
 * @property targetLevel  Nivel al que navegar.
 * @property targetExercise Ejercicio dentro del nivel al que navegar.
 * @property isFirstTime  El usuario nunca usó la app antes.
 * @property hasProgress  El usuario tiene al menos un nivel completado o en curso.
 */
data class ResumeInfo(
    val targetLevel: Int,
    val targetExercise: Int,
    val isFirstTime: Boolean,
    val hasProgress: Boolean
)

class SessionResumeUseCase(private val progressRepository: ProgressRepository) {

    fun getResumeInfo(): ResumeInfo {
        val progress = progressRepository.loadProgress()
        val isFirst = progressRepository.isFirstLaunch()
        return ResumeInfo(
            targetLevel = progress.currentLevel,
            targetExercise = progress.currentExercise,
            isFirstTime = isFirst,
            hasProgress = !isFirst && (progress.currentLevel > 1 || progress.completedLevels.isNotEmpty())
        )
    }
}
