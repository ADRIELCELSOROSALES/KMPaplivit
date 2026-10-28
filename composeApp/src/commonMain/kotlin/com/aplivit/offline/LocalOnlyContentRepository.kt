package com.aplivit.offline

import com.aplivit.core.port.AttemptResult
import com.aplivit.core.port.ContentRepository
import com.aplivit.core.port.ContentSyncResult
import com.aplivit.core.port.FlushResult
import com.aplivit.core.port.ProgressSyncResult
import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto

/**
 * [ContentRepository] del modo offline ([com.aplivit.config.offlineOnly]): nunca hay catálogo del
 * backend ni intentos que sincronizar, así que [com.aplivit.core.domain.usecase.GetLevelsUseCase]
 * cae siempre al JSON local y el progreso queda solo en el espejo local.
 */
class LocalOnlyContentRepository : ContentRepository {

    override fun cachedExercises(): List<RemoteExerciseDto> = emptyList()

    override fun cachedLanguage(): String? = null

    override fun pendingAttemptCount(): Int = 0

    override suspend fun refreshContent(): ContentSyncResult = ContentSyncResult.Offline

    override suspend fun submitAttempt(
        exercise: RemoteExerciseDto,
        givenAnswer: String,
        voiceIsCorrect: Boolean?
    ): AttemptResult {
        val verdict = OfflineAnswerValidator.evaluate(exercise.type, exercise.content, givenAnswer)
            ?: (voiceIsCorrect ?: false)
        return AttemptResult(isCorrect = verdict, sentOnline = false)
    }

    override suspend fun flushPendingAttempts(): FlushResult = FlushResult(skippedOffline = true)

    override suspend fun submitLevelCompleted(levelId: Int): Boolean = false

    override suspend fun syncProgress(): ProgressSyncResult = ProgressSyncResult.Offline
}
