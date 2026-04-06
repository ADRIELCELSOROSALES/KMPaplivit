package com.aplivit.core.domain.usecase

import com.aplivit.core.port.ProgressRepository

/**
 * Encapsula las reglas de navegación entre niveles y ejercicios.
 *
 * Reglas:
 * - Atrás: siempre permitido.
 * - Adelante: solo si el nivel/ejercicio de destino ya fue desbloqueado,
 *   es decir, el usuario lo completó en algún momento previo.
 */
class NavigationUseCase(private val progressRepository: ProgressRepository) {

    /**
     * Verdadero si el usuario puede avanzar desde [level]/[exercise]:
     * - está revisando un nivel ya superado, O
     * - está en el mismo nivel máximo pero en un ejercicio ya superado.
     */
    fun canGoForward(level: Int, exercise: Int): Boolean {
        val progress = progressRepository.loadProgress()
        return level < progress.maxUnlockedLevel ||
               (level == progress.maxUnlockedLevel && exercise < progress.maxUnlockedExercise)
    }

    /** Retroceder siempre está permitido. */
    fun canGoBack(level: Int, exercise: Int): Boolean = true

    /**
     * Calcula el destino al presionar atrás.
     * @return Par (nivel, ejercicio) del paso anterior.
     */
    fun goBack(currentLevel: Int, currentExercise: Int): Pair<Int, Int> = when {
        currentExercise > 1 -> Pair(currentLevel, currentExercise - 1)
        currentLevel > 1   -> Pair(currentLevel - 1, 1)
        else               -> Pair(1, 1)   // ya en el inicio
    }

    /**
     * Calcula el destino al presionar adelante.
     * En la implementación actual (1 ejercicio por nivel) siempre avanza al siguiente nivel.
     * @return Par (nivel, ejercicio) del paso siguiente.
     */
    fun goForward(currentLevel: Int, currentExercise: Int): Pair<Int, Int> =
        Pair(currentLevel + 1, 1)
}
