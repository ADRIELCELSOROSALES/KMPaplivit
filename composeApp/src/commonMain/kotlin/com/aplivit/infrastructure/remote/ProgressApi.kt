package com.aplivit.infrastructure.remote

import com.aplivit.config.apiBaseUrl
import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto
import com.aplivit.infrastructure.remote.dto.StudentProgressDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

/**
 * Progreso del alumno según el backend, que es la fuente de verdad de la secuencia (RF-06/RF-07):
 *  - GET /api/my-progress   -> cuánto lleva completado de la secuencia global.
 *  - GET /api/next-exercise -> cuál es el próximo ejercicio (de ahí sale el nivel donde continuar).
 */
class ProgressApi(private val client: HttpClient) {

    suspend fun getMyProgress(): StudentProgressDto =
        client.get("$apiBaseUrl/api/my-progress").body()

    /** null = el alumno ya resolvió toda la secuencia (el backend responde 404 NoNextExercise). */
    suspend fun getNextExercise(): RemoteExerciseDto? =
        try {
            client.get("$apiBaseUrl/api/next-exercise").body()
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) null else throw e
        }
}
