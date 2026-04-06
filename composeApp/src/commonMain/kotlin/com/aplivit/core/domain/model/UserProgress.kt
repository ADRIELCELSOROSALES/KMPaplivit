package com.aplivit.core.domain.model

data class UserProgress(
    val currentLevel: Int = 1,
    val currentExercise: Int = 1,       // ejercicio actual dentro del nivel
    val maxUnlockedLevel: Int = 1,      // máximo nivel alcanzado (frontera de avance)
    val maxUnlockedExercise: Int = 1,   // máximo ejercicio desbloqueado en maxUnlockedLevel
    val completedLevels: Set<Int> = emptySet(),
    val totalErrors: Int = 0
)
