package com.aplivit.offline

import com.aplivit.core.port.AttemptResult
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ContentRepository
import com.aplivit.core.port.ContentSyncResult
import com.aplivit.core.port.FlushResult
import com.aplivit.infrastructure.nowEpochMillis
import com.aplivit.infrastructure.remote.AttemptApi
import com.aplivit.infrastructure.remote.ContentApi
import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto
import com.aplivit.infrastructure.remote.dto.SyncAttemptItemDto
import com.aplivit.infrastructure.remote.dto.SyncAttemptsRequestDto

/**
 * Implementación offline-first del [ContentRepository].
 *
 * Contenido: la app siempre lee de [ContentCache]; [refreshContent] baja el catálogo del backend
 * (RF-13) solo si hay internet y cambió la versión.
 *
 * Intentos: se validan localmente igual que el server (RF-12), se persisten SIEMPRE en la cola
 * (durabilidad) y se sincronizan cuando hay red (RF-14/15). Nada se pierde si no hay conexión.
 */
class OfflineContentRepository(
    private val contentApi: ContentApi,
    private val attemptApi: AttemptApi,
    private val cache: ContentCache,
    private val queue: AttemptQueue,
    private val connectivity: ConnectivityChecker,
    private val levelMapper: BackendLevelMapper,
    // Tamaño del lote a cachear desde la posición actual del alumno (RF-13). pending-exercises NO
    // tiene cursor/offset: siempre devuelve desde la posición actual hacia adelante, así que se
    // pide UN lote (no se pagina sobre has_more, eso duplicaría). 200 = máximo que acepta el
    // backend; si el pendiente supera 200, se recachea al avanzar (has_more lo indica).
    private val batchLimit: Int = 200
) : ContentRepository {

    override fun cachedExercises(): List<RemoteExerciseDto> = cache.load()?.exercises ?: emptyList()

    override fun pendingAttemptCount(): Int = queue.size()

    override suspend fun refreshContent(): ContentSyncResult {
        if (!connectivity.isConnected()) return ContentSyncResult.Offline

        val version = runCatching { contentApi.getContentVersion() }
            .getOrElse { return ContentSyncResult.Failed(it.message) }

        if (cache.cachedContentVersion() == version.contentVersion) return ContentSyncResult.UpToDate

        val page = try {
            contentApi.getPendingExercises(batchLimit)
        } catch (e: Exception) {
            return ContentSyncResult.Failed(e.message)
        }

        cache.save(CachedContent(version.schemaVersion, version.contentVersion, page.items))
        return ContentSyncResult.Updated(page.items.size, version.contentVersion)
    }

    override suspend fun submitAttempt(
        exercise: RemoteExerciseDto,
        givenAnswer: String,
        voiceIsCorrect: Boolean?
    ): AttemptResult {
        // Veredicto local: los 4 tipos deterministas los evalúa la app; VoiceRecognition (o un
        // tipo desconocido) usa lo que reporta el cliente (el motor de voz), igual que el backend.
        val verdict = OfflineAnswerValidator.evaluate(exercise.type, exercise.content, givenAnswer)
            ?: (voiceIsCorrect ?: false)

        val millis = nowEpochMillis()
        queue.enqueue(
            QueuedAttempt(
                localId = "$millis-${exercise.id}",
                exerciseId = exercise.id,
                givenAnswer = givenAnswer,
                language = exercise.language,
                isCorrect = verdict,
                attemptedAt = Iso8601.fromEpochMillis(millis),
                contentVersion = exercise.contentVersion
            )
        )

        var sentOnline = false
        if (connectivity.isConnected()) {
            sentOnline = flushPendingAttempts().let { it.synced > 0 || it.alreadySynced > 0 }
        }
        return AttemptResult(isCorrect = verdict, sentOnline = sentOnline)
    }

    override suspend fun flushPendingAttempts(): FlushResult {
        if (!connectivity.isConnected()) return FlushResult(skippedOffline = true)

        val pending = queue.snapshot()
        if (pending.isEmpty()) return FlushResult()

        val request = SyncAttemptsRequestDto(
            items = pending.map {
                SyncAttemptItemDto(
                    exerciseId = it.exerciseId,
                    givenAnswer = it.givenAnswer,
                    language = it.language,
                    isCorrect = it.isCorrect,
                    attemptedAt = it.attemptedAt,
                    contentVersion = it.contentVersion
                )
            }
        )

        val response = runCatching { attemptApi.sync(request) }.getOrNull()
            ?: return FlushResult() // error de red: se conserva la cola, se reintenta después

        // El sync es idempotente y best-effort por ítem: tras un 200, el servidor ya procesó todo
        // el lote (sincronizado, ya-sincronizado o error por ítem). Se remueve exactamente lo que
        // se envió; los intentos encolados durante el request quedan para la próxima.
        queue.remove(pending.map { it.localId }.toSet())

        return FlushResult(
            synced = response.syncedCount,
            alreadySynced = response.alreadySyncedCount,
            errors = response.errorCount
        )
    }

    override suspend fun submitLevelCompleted(levelId: Int): Boolean {
        val dto = cache.load()?.exercises
            ?.firstOrNull { levelMapper.toLevel(it)?.id == levelId }
            ?: return false
        // Nivel de tipo VoiceRecognition (client-evaluated): completarlo = intento correcto.
        submitAttempt(dto, givenAnswer = dto.content.targetWord ?: "", voiceIsCorrect = true)
        return true
    }
}
