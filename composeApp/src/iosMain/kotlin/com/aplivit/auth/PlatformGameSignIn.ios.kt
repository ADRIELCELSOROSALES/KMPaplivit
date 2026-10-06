package com.aplivit.auth

/**
 * TODO(auth-nativa): integrar Apple GameKit (GKLocalPlayer.authenticateHandler +
 * fetchItemsForIdentityVerificationSignature) para obtener publicKeyUrl/signature/salt/timestamp
 * y devolver [PlatformSignInResult.GameCenter]. Necesita la capability de Game Center en el target
 * iOS. Hasta entonces el login nativo no está disponible (el resto de la cadena de auth sí).
 */
private class IosGameCenterSignIn : PlatformGameSignIn {
    override suspend fun signIn(): PlatformSignInResult? =
        throw NotImplementedError(
            "Login con Game Center pendiente: integrar GKLocalPlayer + firma de verificación de identidad."
        )
}

actual fun providePlatformGameSignIn(): PlatformGameSignIn = IosGameCenterSignIn()
