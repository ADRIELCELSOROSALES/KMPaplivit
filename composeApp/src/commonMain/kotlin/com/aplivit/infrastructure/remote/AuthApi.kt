package com.aplivit.infrastructure.remote

import com.aplivit.config.apiBaseUrl
import com.aplivit.infrastructure.remote.dto.GameCenterLoginRequestDto
import com.aplivit.infrastructure.remote.dto.LoginResponseDto
import com.aplivit.infrastructure.remote.dto.PlayGamesLoginRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Login del alumno (RF-02). Autoprovisiona la cuenta en el primer ingreso y devuelve el JWT
 * interno que el resto de la API espera en el header Authorization.
 */
class AuthApi(private val client: HttpClient) {

    suspend fun loginWithPlayGames(serverAuthCode: String): LoginResponseDto =
        client.post("$apiBaseUrl/api/auth/student/play-games") {
            setBody(PlayGamesLoginRequestDto(serverAuthCode))
        }.body()

    suspend fun loginWithGameCenter(request: GameCenterLoginRequestDto): LoginResponseDto =
        client.post("$apiBaseUrl/api/auth/student/game-center") {
            setBody(request)
        }.body()
}
