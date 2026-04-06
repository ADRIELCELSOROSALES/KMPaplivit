package com.aplivit.core.domain.usecase

import com.aplivit.core.port.ProgressRepository

class UnlockNextLevelUseCase(private val repository: ProgressRepository) {

    fun execute(completedLevelId: Int) {
        val progress = repository.loadProgress()
        val nextLevel = completedLevelId + 1
        val newMaxLevel = maxOf(progress.maxUnlockedLevel, nextLevel)

        val updated = progress.copy(
            // Advance currentLevel only if the user was exactly at this level
            currentLevel = if (progress.currentLevel == completedLevelId) nextLevel
                           else progress.currentLevel,
            maxUnlockedLevel = newMaxLevel,
            // When a new level is unlocked, its first exercise becomes the new frontier
            maxUnlockedExercise = if (newMaxLevel > progress.maxUnlockedLevel) 1
                                  else progress.maxUnlockedExercise
        )
        repository.saveProgress(updated)
    }
}
