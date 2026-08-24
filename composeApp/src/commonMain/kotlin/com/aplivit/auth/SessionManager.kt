package com.aplivit.auth

import com.aplivit.infrastructure.remote.AuthApi
import com.aplivit.infrastructure.remote.dto.GameCenterLoginRequestDto

/**
 * Orquesta la sesión del alumno: dispara el login nativo, canjea la credencial por el JWT interno
 * y lo persiste. El HttpClient toma el token del [TokenStore] en cada request.
 */
class SessionManager(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val platformSignIn: PlatformGameSignIn
) {

    fun isSignedIn(): Boolean = tokenStore.isSignedIn()

    /** Login completo (nativo -> backend -> token guardado). false si el usuario canceló. */
    suspend fun signIn(): Boolean {
        val credential = platformSignIn.signIn() ?: return false
        val response = when (credential) {
            is PlatformSignInResult.PlayGames ->
                authApi.loginWithPlayGames(credential.serverAuthCode)

            is PlatformSignInResult.GameCenter ->
                authApi.loginWithGameCenter(
                    GameCenterLoginRequestDto(
                        playerId = credential.playerId,
                        publicKeyUrl = credential.publicKeyUrl,
                        signature = credential.signature,
                        salt = credential.salt,
                        timestamp = credential.timestamp,
                        displayName = credential.displayName
                    )
                )
        }
        tokenStore.save(response.token, response.expiresAtUtc)
        return true
    }

    fun signOut() = tokenStore.clear()

    /**
     * Helper de DEV: inyecta un JWT a mano para probar el ciclo sin el flujo nativo (ej. un token
     * de alumno generado con tools/DevTokenGenerator del backend). No usar en producción.
     */
    fun useTokenForDev(jwt: String) = tokenStore.save(jwt, null)
}
