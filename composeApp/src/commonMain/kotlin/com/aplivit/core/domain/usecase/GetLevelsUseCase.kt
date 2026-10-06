package com.aplivit.core.domain.usecase

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.Level
import com.aplivit.core.port.ContentRepository
import com.aplivit.infrastructure.content.LevelsLoader
import com.aplivit.offline.BackendLevelMapper

/**
 * Fuente de niveles de la app. El contenido lo DECIDE el backend: si hay catálogo cacheado se
 * reconstruyen los `Level` desde ahí (offline-first, sobrevive sin conexión). El JSON local
 * empaquetado queda como respaldo del primer arranque sin que nunca haya habido internet, y para
 * completar los niveles ya resueltos (ver [fillCompletedPrefix]).
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
            if (cached.isNotEmpty()) return fillCompletedPrefix(cached, language)
            warnIfCatalogNotMappable()
        }

        // Cache vacío o de otro idioma: traer del backend en el idioma actual ANTES de caer al
        // JSON local (evita el race con el sync y sirve el idioma correcto). No-op sin red/sesión.
        contentRepository.refreshContent()
        if (contentRepository.cachedLanguage() == language.code) {
            val fromBackend = levelMapper.toLevels(contentRepository.cachedExercises())
            if (fromBackend.isNotEmpty()) return fillCompletedPrefix(fromBackend, language)
            warnIfCatalogNotMappable()
        }

        // Respaldo local (levels_<code>.json, ya en el idioma correcto).
        return levelsLoader.load(language)
    }

    /**
     * El catálogo del backend llega de `pending-exercises`: arranca en la posición ACTUAL del
     * alumno, así que no trae los niveles que ya resolvió (y en un dispositivo nuevo eso es casi
     * todo el principio del curso). Sin esto, la lista de niveles del home empezaría en el nivel
     * en curso y el alumno no vería su avance.
     *
     * Los niveles anteriores al primero pendiente se completan con el JSON local del mismo
     * idioma. Solo el prefijo ya resuelto: los que el backend quitó por encima de esa posición
     * siguen fuera de la lista, porque ahí sí sabemos que el backend los excluyó a propósito.
     */
    private suspend fun fillCompletedPrefix(
        fromBackend: List<Level>,
        language: AppLanguage
    ): List<Level> {
        val firstPending = fromBackend.first().id
        if (firstPending <= 1) return fromBackend

        val alreadyDone = runCatching { levelsLoader.load(language) }
            .getOrDefault(emptyList())
            .filter { it.id < firstPending }

        return alreadyDone + fromBackend
    }

    /**
     * Hay catálogo del backend pero NINGÚN ejercicio se pudo convertir en nivel: el contenido
     * publicado no trae el payload de nivel que espera la app (ver tools/content-export). Antes
     * esto caía al JSON local en silencio y parecía que el backend "no cambiaba nada".
     */
    private fun warnIfCatalogNotMappable() {
        val cachedCount = contentRepository.cachedExercises().size
        if (cachedCount > 0) {
            println(
                "CONTENT [GetLevelsUseCase] $cachedCount ejercicios cacheados del backend y 0 " +
                    "niveles mapeables (payload sin appType=LEVEL/word/syllables): se usa el " +
                    "contenido local de respaldo"
            )
        }
    }
}
