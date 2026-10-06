package com.aplivit.auth

/**
 * Credencial obtenida del proveedor nativo, lista para canjearse por un JWT en el backend.
 * Android -> Play Games (server auth code). iOS -> Game Center (bundle firmado de GameKit).
 */
sealed interface PlatformSignInResult {
    data class PlayGames(val serverAuthCode: String) : PlatformSignInResult

    data class GameCenter(
        val playerId: String,
        val publicKeyUrl: String,
        val signature: String,
        val salt: String,
        val timestamp: Long,
        val displayName: String?
    ) : PlatformSignInResult
}

/**
 * Dispara el flujo de login nativo del alumno. La implementación real depende de SDKs nativos
 * (Google Play Games / Apple GameKit) y de credenciales del proyecto, así que hoy es un stub
 * por plataforma; el resto de la cadena de auth ya está lista para usarla.
 */
interface PlatformGameSignIn {
    /** Devuelve la credencial nativa, o null si el usuario canceló. */
    suspend fun signIn(): PlatformSignInResult?
}

expect fun providePlatformGameSignIn(): PlatformGameSignIn
