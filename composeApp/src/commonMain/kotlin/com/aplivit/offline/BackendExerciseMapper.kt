package com.aplivit.offline

import com.aplivit.core.domain.model.VocalizeExercise
import com.aplivit.core.domain.model.VocalizeType
import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto
import com.aplivit.infrastructure.remote.dto.RemoteExerciseType
import kotlinx.serialization.json.Json

/**
 * Ejercicio del backend traducido a un modelo renderizable por la UI de la app, conservando el
 * [source] (dto) porque `submitAttempt` necesita id/type/content/language/contentVersion.
 */
sealed interface MappedExercise {
    val source: RemoteExerciseDto

    data class Vocalize(
        override val source: RemoteExerciseDto,
        val exercise: VocalizeExercise
    ) : MappedExercise

    /** Tipo aún no mapeado a una pantalla (se completa a medida que se autoran los tipos ricos). */
    data class Unsupported(
        override val source: RemoteExerciseDto,
        val reason: String
    ) : MappedExercise
}

/**
 * Traduce backend -> modelos de UI de la app usando el `payload` (schema propio, ver
 * [ExercisePayload]) y, como respaldo, los campos de `content` que el backend sí entiende.
 *
 * Hoy resuelve concretamente el path real (VoiceRecognition / VOCALIZE, que es el contenido
 * cargado). Los otros 4 tipos quedan como [MappedExercise.Unsupported] hasta que se autoren sus
 * variantes ricas y se confirme su payload — la UI decide cómo tratarlos sin romperse.
 */
class BackendExerciseMapper {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun map(dto: RemoteExerciseDto): MappedExercise {
        val payload = dto.content.payload?.let {
            runCatching { json.decodeFromString<ExercisePayload>(it) }.getOrNull()
        }

        val isVocalize = dto.type == RemoteExerciseType.VoiceRecognition ||
            payload?.appType?.equals("VOCALIZE", ignoreCase = true) == true

        if (isVocalize) return mapVocalize(dto, payload)

        return MappedExercise.Unsupported(dto, "Tipo ${dto.type} sin mapeo de UI todavía")
    }

    private fun mapVocalize(dto: RemoteExerciseDto, payload: ExercisePayload?): MappedExercise {
        val content = payload?.content
            ?: dto.content.targetWord
            ?: dto.content.syllables?.firstOrNull()
            ?: ""

        val type = when (payload?.vocalizeType?.uppercase()) {
            "SYLLABLE" -> VocalizeType.SYLLABLE
            "WORD" -> VocalizeType.WORD
            "SENTENCE" -> VocalizeType.SENTENCE
            // Sin payload: inferir por el content del backend (palabra completa vs sílaba suelta).
            else -> if (dto.content.targetWord != null) VocalizeType.WORD else VocalizeType.SYLLABLE
        }

        return MappedExercise.Vocalize(
            source = dto,
            exercise = VocalizeExercise(id = dto.order, type = type, content = content)
        )
    }
}
