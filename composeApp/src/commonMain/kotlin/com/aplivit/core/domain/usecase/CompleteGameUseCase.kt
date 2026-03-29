package com.aplivit.core.domain.usecase

import com.aplivit.core.port.ProgressRepository

class CompleteGameUseCase(private val repository: ProgressRepository) {
    fun execute(levelId: Int, errors: Int) {
        val progress = repository.loadProgress()
        val updated = progress.copy(
            completedLevels = progress.completedLevels + levelId,
            totalErrors = progress.totalErrors + errors
        )
        repository.saveProgress(updated)
    }
}
