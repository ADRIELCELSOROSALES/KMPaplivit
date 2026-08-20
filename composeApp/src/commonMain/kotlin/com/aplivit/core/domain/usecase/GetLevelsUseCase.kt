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
        // Usar el cache solo si es del idioma seleccionado (el cache es por idioma).
        if (contentRepository.cachedLanguage() == language.code) {
            val cached = levelMapper.toLevels(contentRepository.cachedExercises())
            if (cached.isNotEmpty()) return cached
        }

        // Cache vacío o de otro idioma: traer del backend en el idioma actual ANTES de caer al
        // JSON local (evita el race con el sync y sirve el idioma correcto). No-op sin red/sesión.
        contentRepository.refreshContent()
        if (contentRepository.cachedLanguage() == language.code) {
            val fromBackend = levelMapper.toLevels(contentRepository.cachedExercises())
            if (fromBackend.isNotEmpty()) return fromBackend
        }

        // Respaldo local (levels_<code>.json, ya en el idioma correcto).
        return levelsLoader.load(language)
    }
}
