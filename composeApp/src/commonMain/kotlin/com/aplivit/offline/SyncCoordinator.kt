package com.aplivit.offline

import com.aplivit.auth.SessionManager
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ContentRepository

/**
 * Orquesta la sincronización offline-first en un solo lugar, para invocar desde el ciclo de vida
 * (arranque de la app, vuelta de conexión, después de un intento). Es idempotente y seguro de
 * llamar varias veces: si no hay sesión o no hay red, no hace nada y no falla.
 *
 * ── Decisión de progreso (ítem 5) ──
 * El backend es la fuente de verdad de la SECUENCIA y el PROGRESO (next-exercise / my-progress):
 * decide qué ejercicio sigue y el % de avance. El [ContentRepository] local es un espejo para
 * jugar offline; los intentos se acumulan y se reconcilian contra el backend al sincronizar
 * (RF-14/15). El ProgressRepository local (Settings) queda solo como respaldo de UX offline,
 * no como fuente de verdad.
 */
class SyncCoordinator(
    private val session: SessionManager,
    private val content: ContentRepository,
    private val connectivity: ConnectivityChecker
) {

    /** Llamar al abrir la app y al recuperar conexión. Sube lo pendiente y refresca el catálogo. */
    suspend fun sync(): Boolean {
        // Gate de sesión: sin JWT de alumno no hay endpoints StudentOnly que consumir.
        if (!session.isSignedIn()) return false
        if (!connectivity.isConnected()) return false

        // Primero subir lo hecho offline, después traer contenido nuevo.
        content.flushPendingAttempts()
        content.refreshContent()
        return true
    }
}
