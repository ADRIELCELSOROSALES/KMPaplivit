package com.aplivit.offline

import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Contenido cacheado en disco: sobrevive reinicios y permite jugar offline por meses. */
@Serializable
data class CachedContent(
    val schemaVersion: Int,
    val contentVersion: String,
    // Idioma REAL del contenido descargado (código AppLanguage: "es"/"en"/"fr"). Puede no ser el
    // pedido: si el backend no tiene traducción a ese idioma (RF-09b) devuelve el idioma base.
    val language: String,
    val exercises: List<RemoteExerciseDto>,
    // Idioma que la app pidió (preferencia del alumno). Se guarda aparte de [language] para no
    // re-descargar en loop cuando el backend no tiene ese idioma y responde siempre en el base.
    val requestedLanguage: String = language
)

/**
 * Cache persistente del catálogo. La app SIEMPRE lee de acá para jugar; la red solo lo refresca.
 * Se serializa como JSON en multiplatform-settings.
 */
class ContentCache(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): CachedContent? {
        val raw = settings.getStringOrNull(KEY) ?: return null
        return runCatching { json.decodeFromString<CachedContent>(raw) }.getOrNull()
    }

    fun save(content: CachedContent) {
        settings.putString(KEY, json.encodeToString(content))
    }

    /** Versión del catálogo cacheada, o null si nunca se descargó. */
    fun cachedContentVersion(): String? = load()?.contentVersion

    /** Idioma del contenido cacheado, o null si nunca se descargó. */
    fun cachedLanguage(): String? = load()?.language

    /** Idioma con el que se pidió el contenido cacheado, o null si nunca se descargó. */
    fun cachedRequestedLanguage(): String? = load()?.requestedLanguage

    fun clear() {
        settings.remove(KEY)
    }

    private companion object {
        const val KEY = "offline.content.v1"
    }
}
