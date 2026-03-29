package com.aplivit.core.domain.usecase

import com.aplivit.core.domain.model.UserProgress
import com.aplivit.core.port.ProgressRepository

class UnlockNextLevelUseCase(private val repository: ProgressRepository) {
    operator fun invoke(progress: UserProgress, completedLevelId: Int): UserProgress {
        val nextLevel = completedLevelId + 1
        val updated = progress.copy(currentLevel = nextLevel)
        repository.saveProgress(updated)
        return updated
    }
}
