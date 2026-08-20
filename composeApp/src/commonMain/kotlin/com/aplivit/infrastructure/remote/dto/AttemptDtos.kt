package com.aplivit.infrastructure.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request de POST /api/exercise-attempts (registro online de un intento). `language` y
 * `contentVersion` se reenvían tal cual vinieron en el ejercicio (RF-09b / RF-15).
 */
@Serializable
data class RecordAttemptRequestDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("given_answer") val givenAnswer: String,
    @SerialName("language") val language: RemoteLanguage? = null,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("content_version") val contentVersion: Int? = null
)

@Serializable
data class RecordAttemptResponseDto(
    @SerialName("id") val id: String
)

/**
 * Item de POST /api/exercise-attempts/sync (lote offline, RF-14). `attemptedAt` es el momento
 * REAL en el dispositivo, en ISO-8601 (nunca la hora de sincronización).
 */
@Serializable
data class SyncAttemptItemDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("given_answer") val givenAnswer: String,
    @SerialName("language") val language: RemoteLanguage? = null,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("attempted_at") val attemptedAt: String,
    @SerialName("content_version") val contentVersion: Int? = null
)

@Serializable
data class SyncAttemptsRequestDto(
    @SerialName("items") val items: List<SyncAttemptItemDto>
)

@Serializable
data class SyncedAttemptResultDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("attempted_at") val attemptedAt: String,
    @SerialName("status") val status: String,
    @SerialName("error_code") val errorCode: String? = null
)

@Serializable
data class SyncAttemptsResponseDto(
    @SerialName("results") val results: List<SyncedAttemptResultDto>,
    @SerialName("synced_count") val syncedCount: Int,
    @SerialName("already_synced_count") val alreadySyncedCount: Int,
    @SerialName("error_count") val errorCount: Int
)
