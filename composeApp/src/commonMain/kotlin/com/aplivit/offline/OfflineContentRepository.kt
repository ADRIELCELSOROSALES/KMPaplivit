package com.aplivit.offline

import com.aplivit.core.port.AttemptResult
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ContentRepository
import com.aplivit.core.port.ContentSyncResult
import com.aplivit.core.port.FlushResult
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.ProgressSyncResult
import com.aplivit.infrastructure.nowEpochMillis
import com.aplivit.infrastructure.remote.AttemptApi
import com.aplivit.infrastructure.remote.ContentApi
import com.aplivit.infrastructure.remote.MyLanguageApi
import com.aplivit.infrastructure.remote.ProgressApi
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
 *
 * Progreso: el backend es la fuente de verdad (RF-06/RF-07); [syncProgress] lo baja al espejo
 * local para que la app pueda arrancar donde quedó la cuenta y seguir funcionando sin red.
 */
class OfflineContentRepository(
    private val contentApi: ContentApi,
    private val attemptApi: AttemptApi,
    private val cache: ContentCache,
    private val queue: AttemptQueue,
    private val connectivity: ConnectivityChecker,
    private val levelMapper: BackendLevelMapper,
    private val progressRepository: ProgressRepository,
    private val myLanguageApi: MyLanguageApi,
    private val progressApi: ProgressApi,
    // Tamaño del lote a cachear desde la posición actual del alumno (RF-13). pending-exercises NO
    // tiene cursor/offset: siempre devuelve desde la posición actual hacia adelante, así que se
    // pide UN lote (no se pagina sobre has_more, eso duplicaría). 200 = máximo que acepta el
    // backend; si el pendiente supera 200, se recachea al avanzar (has_more lo indica).
    private val batchLimit: Int = 200
) : ContentRepository {

    override fun cachedExercises(): List<RemoteExerciseDto> = cache.load()?.exercises ?: emptyList()

    override fun cachedLanguage(): String? = cache.cachedLanguage()

    override fun pendingAttemptCount(): Int = queue.size()

    override suspend fun refreshContent(): ContentSyncResult {
        if (!connectivity.isConnected()) return ContentSyncResult.Offline

        // El backend sirve el contenido en el idioma de preferencia del alumno (RF-09b): se fija
        // ese idioma ANTES de pedir el contenido, para que venga traducido al idioma seleccionado.
        val language = progressRepository.getSelectedLanguage()
        runCatching { myLanguageApi.setLanguage(language.toRemote()) }

        val version = runCatching { contentApi.getContentVersion() }
            .getOrElse { return ContentSyncResult.Failed(it.message) }

        // Cache válido solo si coinciden versión E idioma pedido (el cache es por idioma). Se
        // compara contra el idioma PEDIDO, no el devuelto: si el backend no tiene traducción a
        // ese idioma responde siempre en el base, y comparar contra el devuelto re-descargaría
        // en cada llamada.
        if (cache.cachedContentVersion() == version.contentVersion &&
            cache.cachedRequestedLanguage() == language.code
        ) {
            return ContentSyncResult.UpToDate
        }

        val page = try {
            contentApi.getPendingExercises(batchLimit)
        } catch (e: Exception) {
            return ContentSyncResult.Failed(e.message)
        }

        // Idioma efectivo del contenido devuelto (RF-09b): si el backend no tiene traducción al
        // idioma del alumno, cae al idioma base. Se cachea con ESE idioma para no mostrar, por
        // ejemplo, contenido en español rotulado como inglés: si no coincide con el pedido,
        // GetLevelsUseCase usa el JSON local de respaldo, que sí está en el idioma correcto.
        val resolvedLanguage = page.items.firstOrNull()?.language?.toAppCodeOrNull() ?: language.code
        cache.save(
            CachedContent(
                schemaVersion = version.schemaVersion,
                contentVersion = version.contentVersion,
                language = resolvedLanguage,
                exercises = page.items,
                requestedLanguage = language.code
            )
        )
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

    override suspend fun syncProgress(): ProgressSyncResult {
        if (!connectivity.isConnected()) return ProgressSyncResult.Offline

        val remote = runCatching { progressApi.getMyProgress() }
            .getOrElse { return ProgressSyncResult.Failed(it.message) }
        val next = runCatching { progressApi.getNextExercise() }
            .getOrElse { return ProgressSyncResult.Failed(it.message) }

        // next == null -> el alumno ya resolvió toda la secuencia publicada.
        val resumeLevel = next?.let { levelMapper.toLevel(it)?.id }
        if (next != null && resumeLevel == null) {
            // El backend tiene contenido que la app no sabe convertir en niveles (payload sin
            // level/word). No se toca el progreso local: se avisa en vez de fingir que está todo
            // hecho. Se arregla del lado del contenido (ver tools/content-export).
            return ProgressSyncResult.Failed(
                "el próximo ejercicio del backend no trae datos de nivel en el payload"
            )
        }

        val language = progressRepository.getSelectedLanguage()
        val local = progressRepository.loadProgress(language)

        // Niveles completados según el backend: la secuencia es global y ordenada, y hay UN
        // ejercicio de backend por nivel (ver tools/content-export), así que todo lo anterior al
        // próximo nivel ya está resuelto. Si no queda próximo, está completa hasta `total`.
        val completedRemotely = when {
            resumeLevel != null -> (1 until resumeLevel).toSet()
            remote.total > 0 -> (1..remote.total).toSet()
            else -> emptySet()
        }

        // Con intentos todavía sin subir, el backend está atrasado a propósito: no se retrocede
        // al alumno a un nivel que ya jugó offline.
        val backendLevel = resumeLevel ?: (completedRemotely.maxOrNull() ?: local.currentLevel)
        val targetLevel = if (queue.size() > 0) maxOf(backendLevel, local.currentLevel) else backendLevel

        progressRepository.saveProgress(
            local.copy(
                currentLevel = targetLevel,
                currentExercise = if (targetLevel != local.currentLevel) 1 else local.currentExercise,
                maxUnlockedLevel = maxOf(local.maxUnlockedLevel, targetLevel),
                completedLevels = local.completedLevels + completedRemotely
            ),
            language
        )

        return ProgressSyncResult.Synced(
            resumeLevel = resumeLevel,
            completed = remote.completed,
            total = remote.total
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
