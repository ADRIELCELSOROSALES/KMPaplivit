package com.aplivit.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.base64EncodedStringWithOptions
import platform.GameKit.GKLocalPlayer
// Viene de una categoría de ObjC: en Kotlin/Native es una propiedad de extensión y necesita
// su propio import, no alcanza con importar GKLocalPlayer.
import platform.GameKit.authenticateHandler
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import kotlin.coroutines.resume

/**
 * Login nativo del alumno con Game Center (RF-02).
 *
 * Son dos pasos: primero autenticar al jugador local (Apple muestra su propia pantalla si hace
 * falta) y después pedirle a Apple la firma de verificación de identidad, que es lo que el backend
 * valida contra el certificado público de Apple.
 *
 * El `teamPlayerID` es el identificador que Apple mete en el payload firmado
 * (playerID + bundleID + timestamp + salt), así que es EXACTAMENTE el que hay que mandar: con
 * `gamePlayerID` o el `playerID` viejo la firma no valida y el backend responde 401.
 *
 * Requiere la capability Game Center en el target y el App ID (ver iosApp.entitlements).
 */
private class IosGameCenterSignIn : PlatformGameSignIn {

    override suspend fun signIn(): PlatformSignInResult? {
        val player = authenticate()
        if (player == null) {
            println("GC [signIn] sin jugador autenticado: no hay credencial que canjear")
            return null
        }
        println("GC [signIn] autenticado, pidiendo la firma de identidad a Apple")
        return fetchIdentityVerification(player)
    }

    /**
     * Dispara el flujo de autenticación de Game Center. El handler de Apple puede llamarse varias
     * veces: con un view controller (hay que mostrarlo y esperar), con error, o sin nada cuando ya
     * quedó autenticado. Devuelve null si el alumno canceló o no hay sesión de Game Center.
     */
    private suspend fun authenticate(): GKLocalPlayer? {
        val player = GKLocalPlayer.localPlayer()
        println("GC [authenticate] isAuthenticated inicial=${player.isAuthenticated()}")
        if (player.isAuthenticated()) return player

        return suspendCancellableCoroutine { continuation ->
            player.authenticateHandler = { viewController: UIViewController?, error: NSError? ->
                println(
                    "GC [handler] viewController=${viewController != null} " +
                        "isAuthenticated=${player.isAuthenticated()} error=${error?.localizedDescription}"
                )
                when {
                    // Apple pide mostrar su pantalla de login: se presenta y se espera a que el
                    // handler vuelva a llamarse con el resultado.
                    viewController != null -> presentSignInScreen(viewController)

                    continuation.isActive -> continuation.resume(
                        player.takeIf { it.isAuthenticated() }
                    )
                }
            }
        }
    }

    /** Credencial firmada por Apple, lista para que el backend la verifique. */
    private suspend fun fetchIdentityVerification(player: GKLocalPlayer): PlatformSignInResult? =
        suspendCancellableCoroutine { continuation ->
            player.fetchItemsForIdentityVerificationSignature { publicKeyUrl, signature, salt, timestamp, error ->
                if (!continuation.isActive) return@fetchItemsForIdentityVerificationSignature

                val url = publicKeyUrl?.absoluteString
                if (url == null || signature == null || salt == null) {
                    println("GC [identity] Apple no devolvio la firma: url=$url error=${error?.localizedDescription}")
                    continuation.resume(null)
                    return@fetchItemsForIdentityVerificationSignature
                }

                println("GC [identity] firma obtenida, teamPlayerID=${player.teamPlayerID}")
                continuation.resume(
                    PlatformSignInResult.GameCenter(
                        playerId = player.teamPlayerID,
                        publicKeyUrl = url,
                        signature = signature.toBase64(),
                        salt = salt.toBase64(),
                        timestamp = timestamp.toLong(),
                        displayName = player.alias
                    )
                )
            }
        }

    private fun presentSignInScreen(viewController: UIViewController) {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        println("GC [present] mostrando la pantalla de Game Center, root=${rootViewController != null}")
        rootViewController?.presentViewController(viewController, animated = true, completion = null)
    }
}

private fun NSData.toBase64(): String = base64EncodedStringWithOptions(0u)

actual fun providePlatformGameSignIn(): PlatformGameSignIn = IosGameCenterSignIn()
