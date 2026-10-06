package com.aplivit.auth

import com.aplivit.infrastructure.remote.AuthApi
import com.aplivit.infrastructure.remote.dto.GameCenterLoginRequestDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orquesta la sesión del alumno: dispara el login nativo, canjea la credencial por el JWT interno
 * y lo persiste. El HttpClient toma el token del [TokenStore] en cada request.
 */
class SessionManager(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val platformSignIn: PlatformGameSignIn
) {

    // Serializa las renovaciones: varios requests en paralelo pueden recibir 401 a la vez y no
    // tiene sentido (ni es seguro) disparar varios logins nativos encadenados.
    private val renewMutex = Mutex()

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

    /**
     * Renueva la sesión después de que el backend rechazó el token (401): descarta el token
     * viejo y vuelve a loguear. Devuelve true si quedó un token usable para reintentar.
     *
     * Lo llama el interceptor del HttpClient, así que puede entrar en paralelo desde varios
     * requests: si otra corrutina ya renovó mientras esperábamos el lock, no se re-loguea.
     */
    suspend fun renewSession(): Boolean {
        val staleToken = tokenStore.token()
        return renewMutex.withLock {
            val current = tokenStore.token()
            if (current != null && current != staleToken) return@withLock true

            tokenStore.clear()
            val signedIn = runCatching { signIn() }.getOrDefault(false)
            // Fallback de dev (null en release): permite seguir probando sin el login nativo.
            if (!signedIn) devAuthToken()?.let { useTokenForDev(it) }
            tokenStore.isSignedIn()
        }
    }

    fun signOut() = tokenStore.clear()

    /**
     * Helper de DEV: inyecta un JWT a mano para probar el ciclo sin el flujo nativo (ver
     * [devAuthToken]). En release no hay token de dev que inyectar.
     */
    fun useTokenForDev(jwt: String) = tokenStore.save(jwt, null)
}
