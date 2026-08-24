package com.aplivit.offline

import com.aplivit.infrastructure.remote.dto.RemoteLanguage
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Un intento resuelto (online u offline) a la espera de confirmarse contra el backend.
 * `localId` identifica la fila de forma única en el dispositivo para poder removerla tras
 * sincronizar sin depender del round-trip exacto del `attemptedAt`.
 */
@Serializable
data class QueuedAttempt(
    val localId: String,
    val exerciseId: String,
    val givenAnswer: String,
    val language: RemoteLanguage?,
    val isCorrect: Boolean?,
    val attemptedAt: String,        // ISO-8601 UTC, momento real del intento
    val contentVersion: Int?
)

/**
 * Cola persistente FIFO de intentos pendientes de sincronizar. Sobrevive reinicios: los intentos
 * hechos sin conexión se acumulan acá y se suben en lote al reconectar (RF-14).
 */
class AttemptQueue(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true }

    fun enqueue(attempt: QueuedAttempt) {
        val current = snapshot().toMutableList()
        current.add(attempt)
        persist(current)
    }

    fun snapshot(): List<QueuedAttempt> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<QueuedAttempt>>(raw) }.getOrElse { emptyList() }
    }

    fun size(): Int = snapshot().size

    /** Remueve las filas ya confirmadas (por localId); conserva las que llegaron después. */
    fun remove(localIds: Set<String>) {
        if (localIds.isEmpty()) return
        persist(snapshot().filterNot { it.localId in localIds })
    }

    fun clear() {
        settings.remove(KEY)
    }

    private fun persist(items: List<QueuedAttempt>) {
        settings.putString(KEY, json.encodeToString(items))
    }

    private companion object {
        const val KEY = "offline.attempt-queue.v1"
    }
}
