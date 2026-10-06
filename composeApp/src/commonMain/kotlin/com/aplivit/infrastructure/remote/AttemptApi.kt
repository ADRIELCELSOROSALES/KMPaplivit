package com.aplivit.infrastructure.remote

import com.aplivit.config.apiBaseUrl
import com.aplivit.infrastructure.remote.dto.RecordAttemptRequestDto
import com.aplivit.infrastructure.remote.dto.RecordAttemptResponseDto
import com.aplivit.infrastructure.remote.dto.SyncAttemptsRequestDto
import com.aplivit.infrastructure.remote.dto.SyncAttemptsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Endpoints de intentos del alumno:
 *  - POST /api/exercise-attempts        -> registro online de a un intento.
 *  - POST /api/exercise-attempts/sync   -> subida en lote de intentos hechos offline (RF-14).
 */
class AttemptApi(private val client: HttpClient) {

    suspend fun record(request: RecordAttemptRequestDto): RecordAttemptResponseDto =
        client.post("$apiBaseUrl/api/exercise-attempts") {
            setBody(request)
        }.body()

    suspend fun sync(request: SyncAttemptsRequestDto): SyncAttemptsResponseDto =
        client.post("$apiBaseUrl/api/exercise-attempts/sync") {
            setBody(request)
        }.body()
}
