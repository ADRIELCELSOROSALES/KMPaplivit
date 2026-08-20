package com.aplivit.core.port

import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto

/**
 * Fuente de contenido y de registro de intentos, offline-first.
 * La app siempre juega contra el cache local; la red solo refresca y sincroniza en background.
 */
interface ContentRepository {

    /** Ejercicios cacheados localmente (lo que la app usa para jugar, con o sin internet). */
    fun cachedExercises(): List<RemoteExerciseDto>

    /** Cantidad de intentos aún sin confirmar contra el backend. */
    fun pendingAttemptCount(): Int

    /** Refresca el cache desde el backend si hay internet y hay una versión nueva. */
    suspend fun refreshContent(): ContentSyncResult

    /**
     * Registra un intento: lo valida localmente (RF-12), lo persiste en la cola y —si hay
     * internet— lo sincroniza. Devuelve el veredicto para dar feedback inmediato.
     */
    suspend fun submitAttempt(
        exercise: RemoteExerciseDto,
        givenAnswer: String,
        voiceIsCorrect: Boolean? = null
    ): AttemptResult

    /** Sube en lote los intentos acumulados offline (RF-14). No-op sin internet o cola vacía. */
    suspend fun flushPendingAttempts(): FlushResult
}

sealed interface ContentSyncResult {
    data object Offline : ContentSyncResult
    data object UpToDate : ContentSyncResult
    data class Updated(val exerciseCount: Int, val contentVersion: String) : ContentSyncResult
    data class Failed(val message: String?) : ContentSyncResult
}

data class AttemptResult(
    val isCorrect: Boolean,
    /** true si se confirmó online al toque; false si quedó encolado para sincronizar después. */
    val sentOnline: Boolean
)

data class FlushResult(
    val synced: Int = 0,
    val alreadySynced: Int = 0,
    val errors: Int = 0,
    val skippedOffline: Boolean = false
)
