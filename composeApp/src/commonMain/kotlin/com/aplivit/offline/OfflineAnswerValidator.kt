package com.aplivit.offline

import com.aplivit.infrastructure.remote.dto.ExerciseContentDto
import com.aplivit.infrastructure.remote.dto.RemoteExerciseType

/**
 * Valida un intento OFFLINE con el mismo veredicto que produciría el backend online.
 * Réplica exacta de `docs/RF-12-contrato-validacion.md` (fuente de verdad: Exercise.EvaluateAnswer
 * del backend). Cualquier divergencia es un bug de confianza silencioso.
 *
 * Reglas: comparación ORDINAL case-insensitive (nunca dependiente del locale del dispositivo —
 * ver el bug de la "i" turca), SIN trim, SIN normalización extra.
 */
object OfflineAnswerValidator {

    /** Tipos que la app sabe evaluar localmente (lista blanca, igual que el backend). */
    private val autoEvaluable = setOf(
        RemoteExerciseType.SyllableRecognition,
        RemoteExerciseType.SyllableMatching,
        RemoteExerciseType.WordBuilding,
        RemoteExerciseType.WordCompletion
    )

    fun isAutoEvaluable(type: RemoteExerciseType): Boolean = type in autoEvaluable

    /**
     * Devuelve el veredicto para los 4 tipos deterministas.
     * Para VoiceRecognition (o cualquier tipo no auto-evaluable) devuelve `null`: el veredicto
     * lo pone el cliente (ej. el motor de voz), igual que hace el backend.
     */
    fun evaluate(type: RemoteExerciseType, content: ExerciseContentDto, givenAnswer: String): Boolean? =
        when (type) {
            RemoteExerciseType.SyllableRecognition ->
                ordinalEquals(givenAnswer, content.correctOption)

            RemoteExerciseType.WordBuilding ->
                ordinalEquals(givenAnswer, content.targetWord)

            RemoteExerciseType.WordCompletion ->
                ordinalEquals(givenAnswer, content.correctSyllable)

            RemoteExerciseType.SyllableMatching ->
                matchesAnyPair(givenAnswer, content)

            // No auto-evaluable: decide el cliente.
            RemoteExerciseType.VoiceRecognition -> null
        }

    // Formato esperado de givenAnswer: "Left|Right". Dato mal formado = incorrecto, nunca error.
    private fun matchesAnyPair(givenAnswer: String, content: ExerciseContentDto): Boolean {
        val parts = givenAnswer.split("|")
        if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) return false

        val pairs = content.pairs ?: return false
        val left = parts[0]
        val right = parts[1]

        return pairs.any { pair ->
            (ordinalEquals(pair.left, left) && ordinalEquals(pair.right, right)) ||
                (ordinalEquals(pair.left, right) && ordinalEquals(pair.right, left))
        }
    }

    /**
     * Equivalente a StringComparison.OrdinalIgnoreCase del backend. `uppercase()` de Kotlin (sin
     * argumento de locale) es invariante al locale del sistema, así que no sufre el bug de la "i"
     * turca. NUNCA usar `equals(other, ignoreCase = true)` a secas.
     */
    private fun ordinalEquals(a: String?, b: String?): Boolean {
        if (a == null || b == null) return false
        return a.uppercase() == b.uppercase()
    }
}
