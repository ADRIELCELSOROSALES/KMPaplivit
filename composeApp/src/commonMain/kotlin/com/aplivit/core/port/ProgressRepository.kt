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
}
