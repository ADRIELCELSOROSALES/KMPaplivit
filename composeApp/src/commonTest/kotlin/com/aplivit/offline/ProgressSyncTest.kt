package com.aplivit.offline

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ProgressSyncResult
import com.aplivit.infrastructure.remote.AttemptApi
import com.aplivit.infrastructure.remote.ContentApi
import com.aplivit.infrastructure.remote.MyLanguageApi
import com.aplivit.infrastructure.remote.ProgressApi
import com.aplivit.infrastructure.remote.createApiHttpClient
import com.aplivit.infrastructure.storage.SettingsProgressRepository
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El progreso lo decide el backend (RF-06/RF-07) y la app lo baja al espejo local para poder
 * arrancar donde quedó la cuenta. Se ejercita contra las respuestas REALES del backend
 * (snake_case, payload del ejercicio como string), no contra mocks del repositorio.
 */
class ProgressSyncTest {

    private fun exerciseJson(level: Int, word: String = "MESA", withLevelPayload: Boolean = true): String {
        val payload = if (withLevelPayload) {
            """{"appType":"LEVEL","level":$level,"word":"$word","syllables":["ME","SA"],"instruction":"Escuchá y repetí.","language":"es"}"""
        } else {
            // Bundle granular viejo: sin word/level, la app no puede armar un nivel.
            """{"appType":"VOCALIZE","vocalizeType":"SYLLABLE","content":"ME"}"""
        }
        val content = if (withLevelPayload) {
            """"target_word":"$word","syllables":["ME","SA"],"""
        } else {
            """"syllables":["ME"],"""
        }
        return """{"id":"11111111-1111-1111-1111-111111111111","type":"VoiceRecognition","order":$level,""" +
            """"content":{$content"payload":"${payload.replace("\"", "\\\"")}"},""" +
            """"difficulty_level":"Beginner","language":"Spanish","content_version":1}"""
    }

    private fun progressJson(completed: Int, total: Int) =
        """{"student_id":"97e6a03b-7d45-4692-9553-a11beb1d22e2","completed":$completed,"total":$total,""" +
            """"by_difficulty":[{"difficulty_level":"Beginner","completed":$completed,"total":$total}]}"""

    private class AlwaysConnected(private val connected: Boolean = true) : ConnectivityChecker {
        override fun isConnected(): Boolean = connected
    }

    private fun repository(
        settings: MapSettings = MapSettings(),
        nextExercise: String? = exerciseJson(level = 6),
        myProgress: String = progressJson(completed = 5, total = 45),
        contentLanguage: String = "Spanish"
    ): Pair<OfflineContentRepository, SettingsProgressRepository> {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/api/my-language") -> respond("", HttpStatusCode.NoContent)

                path.endsWith("/api/content/version") ->
                    respondJson("""{"schemaVersion":1,"contentVersion":"v1"}""")

                path.endsWith("/api/pending-exercises") ->
                    respondJson(
                        """{"items":[${exerciseJson(level = 6).replace("\"language\":\"Spanish\"", "\"language\":\"$contentLanguage\"")}],"has_more":false}"""
                    )

                path.endsWith("/api/next-exercise") ->
                    if (nextExercise == null) respond("", HttpStatusCode.NotFound)
                    else respondJson(nextExercise)

                path.endsWith("/api/my-progress") -> respondJson(myProgress)

                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = createApiHttpClient(engine, tokenProvider = { "token-de-prueba" })
        val progressRepository = SettingsProgressRepository(settings)

        return OfflineContentRepository(
            contentApi = ContentApi(client),
            attemptApi = AttemptApi(client),
            cache = ContentCache(settings),
            queue = AttemptQueue(settings),
            connectivity = AlwaysConnected(),
            levelMapper = BackendLevelMapper(),
            progressRepository = progressRepository,
            myLanguageApi = MyLanguageApi(client),
            progressApi = ProgressApi(client)
        ) to progressRepository
    }

    @Test
    fun baja_el_nivel_donde_quedo_la_cuenta_y_marca_los_anteriores_como_hechos() = runTest {
        val (repository, progressRepository) = repository()

        val result = repository.syncProgress()

        assertEquals(ProgressSyncResult.Synced(resumeLevel = 6, completed = 5, total = 45), result)
        val progress = progressRepository.loadProgress(AppLanguage.SPANISH)
        assertEquals(6, progress.currentLevel)
        assertEquals(6, progress.maxUnlockedLevel)
        assertEquals((1..5).toSet(), progress.completedLevels)
        assertEquals(1, progress.currentExercise)
    }

    @Test
    fun sin_proximo_ejercicio_la_secuencia_queda_completa() = runTest {
        val (repository, progressRepository) = repository(
            nextExercise = null,
            myProgress = progressJson(completed = 45, total = 45)
        )

        val result = repository.syncProgress()

        assertEquals(ProgressSyncResult.Synced(resumeLevel = null, completed = 45, total = 45), result)
        val progress = progressRepository.loadProgress(AppLanguage.SPANISH)
        assertEquals((1..45).toSet(), progress.completedLevels)
        assertEquals(45, progress.currentLevel)
    }

    @Test
    fun contenido_sin_datos_de_nivel_falla_en_vez_de_pisar_el_progreso() = runTest {
        val (repository, progressRepository) = repository(
            nextExercise = exerciseJson(level = 6, withLevelPayload = false)
        )

        val result = repository.syncProgress()

        assertTrue(result is ProgressSyncResult.Failed, "esperaba Failed, fue $result")
        // El espejo local queda intacto: no se marca nada como completado.
        val progress = progressRepository.loadProgress(AppLanguage.SPANISH)
        assertEquals(1, progress.currentLevel)
        assertTrue(progress.completedLevels.isEmpty())
    }

    @Test
    fun el_cache_guarda_el_idioma_que_devolvio_el_backend_no_el_pedido() = runTest {
        val settings = MapSettings()
        val (repository, progressRepository) = repository(settings = settings)
        progressRepository.saveSelectedLanguage(AppLanguage.ENGLISH)

        repository.refreshContent()

        // El backend no tenía traducción al inglés y devolvió el idioma base: el cache queda
        // rotulado "es" (así la app usa el JSON local en inglés) pero recuerda que pidió "en"
        // para no re-descargar en cada carga.
        val cache = ContentCache(settings)
        assertEquals("es", cache.cachedLanguage())
        assertEquals("en", cache.cachedRequestedLanguage())
    }
}

private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(body: String) =
    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
