package com.aplivit.offline

import com.aplivit.auth.SessionManager
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ContentRepository
import kotlinx.coroutines.delay

/**
 * Orquesta la sincronización offline-first en un solo lugar, para invocar desde el ciclo de vida
 * (arranque de la app, vuelta de conexión, después de un intento). Es idempotente y seguro de
 * llamar varias veces: si no hay sesión o no hay red, no hace nada y no falla.
 *
 * ── Decisión de progreso ──
 * El backend es la fuente de verdad de la SECUENCIA y el PROGRESO (next-exercise / my-progress):
 * decide qué ejercicio sigue y cuánto lleva completado. El espejo local (Settings) existe para
 * jugar sin conexión, y en cada sync se reconcilia contra el backend —después de subir los
 * intentos pendientes, para que el backend ya tenga en cuenta lo jugado offline (RF-14/15)—.
 * Así, al loguearse en un dispositivo nuevo, el alumno retoma donde dejó.
 */
class SyncCoordinator(
    private val session: SessionManager,
    private val content: ContentRepository,
    private val connectivity: ConnectivityChecker,
    private val accountGuard: AccountGuard
) {

    /** Llamar al abrir la app y al recuperar conexión. Sube lo pendiente y refresca el catálogo. */
    suspend fun sync(): Boolean {
        // Gate de sesión: sin JWT de alumno no hay endpoints StudentOnly que consumir.
        if (!session.isSignedIn()) return false

        // Antes que nada: si el token es de otra cuenta, el espejo local no es de este alumno.
        // Va incluso sin red, para no mostrarle el progreso de otro mientras está offline.
        accountGuard.resetIfAccountChanged()

        // En el arranque en frío ConnectivityManager puede reportar "sin red" durante los primeros
        // ms del proceso (falso negativo): se reintenta brevemente antes de rendirse.
        if (!awaitConnectivity()) return false

        // Orden: subir lo hecho offline -> traer contenido nuevo -> reconciliar el progreso
        // (necesita el catálogo fresco para resolver el nivel del próximo ejercicio).
        val flush = content.flushPendingAttempts()
        val refresh = content.refreshContent()
        val progress = content.syncProgress()

        // Una línea por sync: es el único punto donde se ve si la app está realmente hablando con
        // el backend (el logging de Ktor no sale por logcat en Android).
        println(
            "SYNC [SyncCoordinator] intentos=${flush.synced}/${flush.errors}err " +
                "contenido=$refresh progreso=$progress"
        )
        return true
    }

    private suspend fun awaitConnectivity(attempts: Int = 4, delayMs: Long = 500): Boolean {
        repeat(attempts) { i ->
            if (connectivity.isConnected()) return true
            if (i < attempts - 1) delay(delayMs)
        }
        return connectivity.isConnected()
    }
}
