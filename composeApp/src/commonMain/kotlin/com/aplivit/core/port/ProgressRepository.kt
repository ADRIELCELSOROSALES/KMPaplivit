package com.aplivit.core.port

import com.aplivit.core.domain.model.UserProgress

interface ProgressRepository {
    fun loadProgress(): UserProgress
    fun saveProgress(progress: UserProgress)
}
