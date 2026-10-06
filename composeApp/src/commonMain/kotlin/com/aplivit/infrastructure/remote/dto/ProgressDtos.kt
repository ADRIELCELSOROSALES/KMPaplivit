package com.aplivit.infrastructure.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Espejo de StudentProgressResponse (GET /api/my-progress, RF-07): posición del alumno en la
 * secuencia global calculada por el backend a partir de sus intentos.
 */
@Serializable
data class StudentProgressDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("completed") val completed: Int,
    @SerialName("total") val total: Int,
    @SerialName("by_difficulty") val byDifficulty: List<DifficultyProgressDto> = emptyList()
)

@Serializable
data class DifficultyProgressDto(
    @SerialName("difficulty_level") val difficultyLevel: RemoteDifficultyLevel,
    @SerialName("completed") val completed: Int,
    @SerialName("total") val total: Int
)
