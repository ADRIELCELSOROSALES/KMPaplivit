package com.aplivit.offline

import com.aplivit.auth.TokenStore
import com.aplivit.core.port.ProgressRepository
import com.russhwolf.settings.Settings

/**
 * El progreso, el catálogo cacheado y la cola de intentos son un espejo del alumno logueado, no
 * del dispositivo. Si el JWT pasa a ser de OTRA cuenta (teléfono compartido, cambio de usuario),
 * ese espejo no le pertenece: se limpia ANTES de sincronizar, para no mostrarle el avance de otro
 * ni subirle intentos ajenos con su token.
 *
 * El progreso real se vuelve a bajar del backend en el mismo sync ([com.aplivit.core.port.ContentRepository.syncProgress]).
 */
class AccountGuard(
    private val settings: Settings,
    private val tokenStore: TokenStore,
    private val cache: ContentCache,
    private val queue: AttemptQueue,
    private val progressRepository: ProgressRepository
) {

    /** Limpia el espejo local si el token es de otra cuenta. Devuelve true si hubo que limpiar. */
    fun resetIfAccountChanged(): Boolean {
        val current = tokenStore.studentId() ?: return false
        val previous = settings.getStringOrNull(KEY_LAST_STUDENT_ID)
        settings.putString(KEY_LAST_STUDENT_ID, current)

        // previous == null: primera vez que se registra la cuenta (instalación nueva, o versión
        // anterior de la app). No se borra nada: el progreso local es de este mismo alumno.
        if (previous == null || previous == current) return false

        cache.clear()
        queue.clear()
        progressRepository.resetProgress()
        return true
    }

    private companion object {
        const val KEY_LAST_STUDENT_ID = "auth.account.last.v1"
    }
}
