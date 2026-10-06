package com.aplivit.infrastructure.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo del enum ExerciseType del backend. Se serializa por nombre exacto ("SyllableRecognition"…),
 * igual que lo emite el backend (JsonStringEnumConverter).
 */
@Serializable
enum class RemoteExerciseType {
    SyllableRecognition,
    SyllableMatching,
    WordBuilding,
    WordCompletion,
    VoiceRecognition
}

/** Espejo del enum Language del backend (RF-09). */
@Serializable
enum class RemoteLanguage {
    Spanish,
    English,
    French,
    HaitianCreole
}

/** Espejo del enum DifficultyLevel del backend. */
@Serializable
enum class RemoteDifficultyLevel {
    Beginner,
    Intermediate,
    Advanced
}

/**
 * Espejo de ExerciseContentResponse (snake_case). `payload` es el JSON opaco (string) con los
 * campos ricos propios de la app; el backend nunca lo interpreta.
 */
@Serializable
data class ExerciseContentDto(
    @SerialName("options") val options: List<String>? = null,
    @SerialName("correct_option") val correctOption: String? = null,
    @SerialName("pairs") val pairs: List<SyllablePairDto>? = null,
    @SerialName("syllables") val syllables: List<String>? = null,
    @SerialName("target_word") val targetWord: String? = null,
    @SerialName("word_with_gap") val wordWithGap: String? = null,
    @SerialName("correct_syllable") val correctSyllable: String? = null,
    @SerialName("payload") val payload: String? = null
)

@Serializable
data class SyllablePairDto(
    @SerialName("left") val left: String,
    @SerialName("right") val right: String
)

/**
 * Espejo de NextExerciseResponse — también el item de PendingExercisesResponse.
 * `content` ya viene resuelto en el idioma del alumno (RF-09b). `language` y `contentVersion`
 * DEBEN reenviarse tal cual al registrar/sincronizar el intento (RF-09b / RF-15).
 */
@Serializable
data class RemoteExerciseDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: RemoteExerciseType,
    @SerialName("order") val order: Int,
    @SerialName("content") val content: ExerciseContentDto,
    @SerialName("difficulty_level") val difficultyLevel: RemoteDifficultyLevel,
    @SerialName("language") val language: RemoteLanguage,
    @SerialName("content_version") val contentVersion: Int
)

/** Espejo de PendingExercisesResponse (RF-13). */
@Serializable
data class PendingExercisesDto(
    @SerialName("items") val items: List<RemoteExerciseDto>,
    @SerialName("has_more") val hasMore: Boolean
)

/**
 * Espejo de ContentVersionResponse. OJO: este nivel usa camelCase (excepción deliberada del
 * backend, igual que el bundle). `contentVersion` (string/hash del catálogo) es distinto del
 * `content_version` (int) por ejercicio.
 */
@Serializable
data class ContentVersionDto(
    @SerialName("schemaVersion") val schemaVersion: Int,
    @SerialName("contentVersion") val contentVersion: String
)
