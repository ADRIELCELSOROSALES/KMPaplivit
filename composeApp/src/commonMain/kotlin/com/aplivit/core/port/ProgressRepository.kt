package com.aplivit.core.port

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.UserProgress

interface ProgressRepository {
    fun loadProgress(): UserProgress
    fun saveProgress(progress: UserProgress)
    fun loadProgress(language: AppLanguage): UserProgress
    fun saveProgress(progress: UserProgress, language: AppLanguage)
    fun getSelectedLanguage(): AppLanguage
    fun saveSelectedLanguage(language: AppLanguage)

    /** Returns true only on the very first launch of the app. */
    fun isFirstLaunch(): Boolean

    /** Call once to record that the app has been opened at least once. */
    fun markLaunched()

    /**
     * Borra el progreso guardado de TODOS los idiomas, dejando el idioma seleccionado y el flag
     * de primer arranque. Se usa cuando el dispositivo pasa a ser de otra cuenta: el progreso
     * local es un espejo del alumno logueado, no del teléfono.
     */
    fun resetProgress()
}
