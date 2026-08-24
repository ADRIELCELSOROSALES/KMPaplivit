package com.aplivit.infrastructure.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP compartido contra el backend aplivlit. El engine llega por plataforma
 * (OkHttp en Android, Darwin en iOS) vía [com.aplivit.infrastructure.provideHttpClientEngine].
 *
 * El Bearer se resuelve por request desde [tokenProvider] (el JWT del alumno guardado en
 * TokenStore). Sin token (ej. antes de loguearse, o en los endpoints AllowAnonymous de login)
 * simplemente no se manda el header.
 */
fun createApiHttpClient(
    engine: HttpClientEngine,
    tokenProvider: () -> String?
): HttpClient =
    HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
