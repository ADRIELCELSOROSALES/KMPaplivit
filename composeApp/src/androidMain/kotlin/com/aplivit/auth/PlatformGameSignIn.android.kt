package com.aplivit.auth

/**
 * TODO(auth-nativa): integrar Google Play Games Services v2 para obtener el `serverAuthCode`
 * (requestServerSideAccess) y devolver [PlatformSignInResult.PlayGames]. Necesita el OAuth
 * client id web del proyecto configurado en Play Console + la dependencia play-services-games-v2.
 * Hasta entonces el login nativo no está disponible (el resto de la cadena de auth sí).
 */
private class AndroidPlayGamesSignIn : PlatformGameSignIn {
    override suspend fun signIn(): PlatformSignInResult? =
        throw NotImplementedError(
            "Login con Play Games pendiente: integrar play-services-games-v2 y requestServerSideAccess."
        )
}

actual fun providePlatformGameSignIn(): PlatformGameSignIn = AndroidPlayGamesSignIn()
