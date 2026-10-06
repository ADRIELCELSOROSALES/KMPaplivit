package com.aplivit.infrastructure.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
 *
 * Si el backend responde 401 —token vencido, rotado o de una sesión que ya no vale— se llama a
 * [onUnauthorized] para renovar la sesión y se reintenta el request UNA sola vez con el token
 * nuevo. Sin eso, un token vencido dejaba a la app "muda": todos los requests fallaban y los
 * errores se perdían en los runCatching de la capa offline.
 */
fun createApiHttpClient(
    engine: HttpClientEngine,
    tokenProvider: () -> String?,
    onUnauthorized: suspend () -> Boolean = { false }
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
    }.apply {
        plugin(HttpSend).intercept { request ->
            val call = runCatching { execute(request) }

            // Un 401 en el propio login ES la respuesta (credencial inválida): no se reintenta,
            // o se entraría en un ciclo login -> 401 -> login.
            if (!call.isUnauthorized() || request.isAuthRequest()) {
                return@intercept call.getOrThrow()
            }

            if (!onUnauthorized()) return@intercept call.getOrThrow()
            val renewedToken = tokenProvider() ?: return@intercept call.getOrThrow()

            // defaultRequest ya corrió para este request: el header viejo se reemplaza a mano.
            request.headers[HttpHeaders.Authorization] = "Bearer $renewedToken"
            execute(request)
        }
    }

/**
 * true si la respuesta fue 401, ya sea que haya llegado como respuesta o como excepción
 * (con `expectSuccess = true`, Ktor valida dentro de `execute` y tira [ResponseException]).
 */
private fun Result<HttpClientCall>.isUnauthorized(): Boolean {
    getOrNull()?.let { return it.response.status == HttpStatusCode.Unauthorized }
    val failure = exceptionOrNull()
    return failure is ResponseException && failure.response.status == HttpStatusCode.Unauthorized
}

/** Endpoints de login (AllowAnonymous): quedan fuera del reintento por 401. */
private fun HttpRequestBuilder.isAuthRequest(): Boolean =
    url.pathSegments.filter { it.isNotEmpty() }.take(2) == AUTH_PATH_SEGMENTS

private val AUTH_PATH_SEGMENTS = listOf("api", "auth")
