package com.aplivit.core.domain.usecase

import com.aplivit.core.port.ProgressRepository

class UnlockNextLevelUseCase(private val repository: ProgressRepository) {
    fun execute(completedLevelId: Int) {
        val progress = repository.loadProgress()
        if (progress.currentLevel == completedLevelId) {
            val updated = progress.copy(currentLevel = progress.currentLevel + 1)
            repository.saveProgress(updated)
        }
    }
}
