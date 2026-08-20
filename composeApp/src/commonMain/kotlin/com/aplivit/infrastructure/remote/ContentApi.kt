package com.aplivit.infrastructure.remote

import com.aplivit.config.apiBaseUrl
import com.aplivit.infrastructure.remote.dto.ContentVersionDto
import com.aplivit.infrastructure.remote.dto.PendingExercisesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Endpoints de contenido del backend usados por la app (offline-first):
 *  - GET /api/content/version      -> saber si hay contenido nuevo sin bajar todo.
 *  - GET /api/pending-exercises    -> descarga en lote del catálogo pendiente (RF-13).
 */
class ContentApi(private val client: HttpClient) {

    suspend fun getContentVersion(): ContentVersionDto =
        client.get("$apiBaseUrl/api/content/version").body()

    /** Un lote de ejercicios pendientes desde la posición actual del alumno hacia adelante. */
    suspend fun getPendingExercises(limit: Int = 50): PendingExercisesDto =
        client.get("$apiBaseUrl/api/pending-exercises") {
            parameter("limit", limit)
        }.body()
}
