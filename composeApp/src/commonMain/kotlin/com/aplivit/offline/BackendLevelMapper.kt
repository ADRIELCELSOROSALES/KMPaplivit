package com.aplivit.offline

import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.model.Syllable
import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto
import kotlinx.serialization.json.Json

/**
 * Reconstruye los `Level` de la app desde el catálogo del backend: cada ejercicio de backend =
 * un nivel, con {palabra, sílabas, instrucción} en el `payload`. Así el backend DECIDE el
 * contenido y su orden, y la app genera los mini-juegos con el mismo formato de siempre.
 */
class BackendLevelMapper {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun toLevels(exercises: List<RemoteExerciseDto>): List<Level> =
        exercises.mapNotNull { toLevel(it) }.sortedBy { it.id }

    fun toLevel(dto: RemoteExerciseDto): Level? {
        val payload = dto.content.payload?.let {
            runCatching { json.decodeFromString<ExercisePayload>(it) }.getOrNull()
        }

        val word = payload?.word ?: dto.content.targetWord ?: return null
        val syllables = payload?.syllables ?: dto.content.syllables ?: emptyList()
        val instruction = payload?.instruction ?: ""
        val levelId = payload?.level ?: dto.order

        return Level(
            id = levelId,
            syllables = syllables.map { Syllable(it) },
            word = word,
            instruction = instruction
        )
    }
}
