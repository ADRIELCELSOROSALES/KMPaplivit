package com.aplivit.core.domain.usecase

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.Level
import com.aplivit.core.port.ContentRepository
import com.aplivit.infrastructure.content.LevelsLoader
import com.aplivit.offline.BackendLevelMapper

/**
 * Fuente de niveles de la app. El contenido lo DECIDE el backend: si hay catálogo cacheado se
 * reconstruyen los `Level` desde ahí (offline-first, sobrevive sin conexión). El JSON local
 * empaquetado queda solo como respaldo del primer arranque sin que nunca haya habido internet.
 */
class GetLevelsUseCase(
    private val levelsLoader: LevelsLoader,
    private val contentRepository: ContentRepository,
    private val levelMapper: BackendLevelMapper
) {
    suspend fun execute(language: AppLanguage = AppLanguage.SPANISH): List<Level> {
        val fromBackend = levelMapper.toLevels(contentRepository.cachedExercises())
        if (fromBackend.isNotEmpty()) return fromBackend
        return levelsLoader.load(language)
    }
}
