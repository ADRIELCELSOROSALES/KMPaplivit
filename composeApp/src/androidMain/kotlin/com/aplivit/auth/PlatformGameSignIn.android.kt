package com.aplivit.auth

import com.aplivit.AppContext
import com.google.android.gms.games.PlayGames
import kotlinx.coroutines.tasks.await

/**
 * Login nativo con Google Play Games Services v2.
 *
 * Pide un `server_auth_code` de un solo uso con [requestServerSideAccess], pasándole el **OAuth web
 * client id** del proyecto de Google Cloud vinculado a Play Games Services. El backend lo canjea
 * server-side (ClientId+Secret) por un access token y resuelve el player → JWT interno.
 *
 * El client id **no es secreto** (viaja en cualquier app cliente); el secreto vive solo en el backend.
 */
private const val WEB_CLIENT_ID =
    "274074848157-hbqjocoujgftpelfd5bpuatkrtdjn96p.apps.googleusercontent.com"

private class AndroidPlayGamesSignIn : PlatformGameSignIn {

    override suspend fun signIn(): PlatformSignInResult? {
        // requestServerSideAccess exige una Activity viva (no el applicationContext).
        val activity = AppContext.activity ?: return null
        val signInClient = PlayGames.getGamesSignInClient(activity)

        // Play Games v2 auto-intenta el sign-in al iniciar el SDK; si aún no está autenticado,
        // lo disparamos explícitamente. Si el usuario cancela, queda no-autenticado -> null.
        val authenticated = signInClient.isAuthenticated.await().isAuthenticated ||
            signInClient.signIn().await().isAuthenticated
        if (!authenticated) return null

        // forceRefreshToken=false: reutiliza el consentimiento ya otorgado cuando aplica.
        val serverAuthCode = signInClient.requestServerSideAccess(WEB_CLIENT_ID, false).await()
        return PlatformSignInResult.PlayGames(serverAuthCode)
    }
}

actual fun providePlatformGameSignIn(): PlatformGameSignIn = AndroidPlayGamesSignIn()
