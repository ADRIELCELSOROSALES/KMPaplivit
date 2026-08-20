package com.aplivit.infrastructure.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request de POST /api/auth/student/play-games (RF-02, Android). */
@Serializable
data class PlayGamesLoginRequestDto(
    @SerialName("server_auth_code") val serverAuthCode: String
)

/** Request de POST /api/auth/student/game-center (RF-02, iOS). */
@Serializable
data class GameCenterLoginRequestDto(
    @SerialName("player_id") val playerId: String,
    @SerialName("public_key_url") val publicKeyUrl: String,
    @SerialName("signature") val signature: String,
    @SerialName("salt") val salt: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("display_name") val displayName: String? = null
)

/** Respuesta de ambos logins: el JWT interno + su expiración (camelCase). */
@Serializable
data class LoginResponseDto(
    @SerialName("token") val token: String,
    @SerialName("expiresAtUtc") val expiresAtUtc: String
)
