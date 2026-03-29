package com.aplivit.core.domain.model

data class UserProgress(
    val currentLevel: Int = 1,
    val completedLevels: Set<Int> = emptySet(),
    val totalErrors: Int = 0
)
