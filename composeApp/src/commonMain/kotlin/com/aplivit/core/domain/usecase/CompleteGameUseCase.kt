package com.aplivit.core.domain.usecase

import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.port.ProgressRepository

class CompleteGameUseCase(private val repository: ProgressRepository) {
    operator fun invoke(progress: UserProgress, levelId: Int, errors: Int): UserProgress {
        val updated = progress.copy(
            completedLevels = progress.completedLevels + levelId,
            totalErrors = progress.totalErrors + errors
        )
        repository.saveProgress(updated)
        return updated
    }
}
