package com.aplivit.offline

import com.aplivit.infrastructure.remote.dto.ExerciseContentDto
import com.aplivit.infrastructure.remote.dto.RemoteDifficultyLevel
import com.aplivit.infrastructure.remote.dto.RemoteExerciseType
import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto
import com.aplivit.infrastructure.remote.dto.RemoteLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El contrato entre el contenido publicado y la app: un ejercicio de backend por nivel, con el
 * nivel en `payload` (lo que emite tools/content-export/export.py). Si esto se rompe, la app
 * ignora el catálogo del backend y juega con el JSON local.
 */
class BackendLevelMapperTest {

    private val mapper = BackendLevelMapper()

    private fun exercise(
        id: String = "e1",
        order: Int = 7,
        content: ExerciseContentDto
    ) = RemoteExerciseDto(
        id = id,
        type = RemoteExerciseType.VoiceRecognition,
        order = order,
        content = content,
        difficultyLevel = RemoteDifficultyLevel.Beginner,
        language = RemoteLanguage.Spanish,
        contentVersion = 1
    )

    @Test
    fun mapea_el_payload_de_nivel_que_emite_el_exportador() {
        val payload = """
            {"appType":"LEVEL","level":3,"word":"MESA","syllables":["ME","SA"],
             "instruction":"Escuchá y repetí.","language":"es"}
        """.trimIndent()

        val level = mapper.toLevel(
            exercise(content = ExerciseContentDto(targetWord = "MESA", syllables = listOf("ME", "SA"), payload = payload))
        )

        assertEquals(3, level?.id)
        assertEquals("MESA", level?.word)
        assertEquals(listOf("ME", "SA"), level?.syllables?.map { it.text })
        assertEquals("Escuchá y repetí.", level?.instruction)
    }

    @Test
    fun sin_payload_usa_los_campos_del_backend_y_el_order_como_nivel() {
        val level = mapper.toLevel(
            exercise(order = 12, content = ExerciseContentDto(targetWord = "LUNA", syllables = listOf("LU", "NA")))
        )

        assertEquals(12, level?.id)
        assertEquals("LUNA", level?.word)
    }

    @Test
    fun un_ejercicio_sin_palabra_no_es_un_nivel() {
        // Es el caso del bundle granular viejo (una vocalización por sílaba, sin word/level): no
        // se puede reconstruir un nivel y el catálogo entero queda inservible para la app.
        val payload = """{"appType":"VOCALIZE","vocalizeType":"SYLLABLE","content":"MA"}"""

        val level = mapper.toLevel(
            exercise(content = ExerciseContentDto(syllables = listOf("MA"), payload = payload))
        )

        assertNull(level)
    }

    @Test
    fun ordena_por_nivel_y_descarta_lo_que_no_mapea() {
        val levels = mapper.toLevels(
            listOf(
                exercise(id = "b", content = ExerciseContentDto(payload = """{"level":2,"word":"OSO","syllables":["O","SO"]}""")),
                exercise(id = "a", content = ExerciseContentDto(payload = """{"level":1,"word":"MAMA","syllables":["MA","MA"]}""")),
                exercise(id = "x", content = ExerciseContentDto(syllables = listOf("MA")))
            )
        )

        assertEquals(listOf(1, 2), levels.map { it.id })
        assertTrue(levels.none { it.word.isBlank() })
    }
}
