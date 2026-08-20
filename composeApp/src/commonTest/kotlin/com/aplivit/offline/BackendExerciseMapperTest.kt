package com.aplivit.offline

import com.aplivit.core.domain.model.VocalizeType
import com.aplivit.infrastructure.remote.dto.ExerciseContentDto
import com.aplivit.infrastructure.remote.dto.RemoteDifficultyLevel
import com.aplivit.infrastructure.remote.dto.RemoteExerciseDto
import com.aplivit.infrastructure.remote.dto.RemoteExerciseType
import com.aplivit.infrastructure.remote.dto.RemoteLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BackendExerciseMapperTest {

    private val mapper = BackendExerciseMapper()

    private fun dto(
        type: RemoteExerciseType,
        content: ExerciseContentDto,
        order: Int = 1
    ) = RemoteExerciseDto(
        id = "ex-$order",
        type = type,
        order = order,
        content = content,
        difficultyLevel = RemoteDifficultyLevel.Beginner,
        language = RemoteLanguage.Spanish,
        contentVersion = 1
    )

    @Test
    fun vocalize_fromPayload() {
        val payload = """{"appType":"VOCALIZE","vocalizeType":"WORD","content":"MAMA"}"""
        val result = mapper.map(dto(RemoteExerciseType.VoiceRecognition, ExerciseContentDto(payload = payload)))
        val v = assertIs<MappedExercise.Vocalize>(result)
        assertEquals(VocalizeType.WORD, v.exercise.type)
        assertEquals("MAMA", v.exercise.content)
    }

    @Test
    fun vocalize_inferredWord_whenNoPayload() {
        val result = mapper.map(dto(RemoteExerciseType.VoiceRecognition, ExerciseContentDto(targetWord = "CASA")))
        val v = assertIs<MappedExercise.Vocalize>(result)
        assertEquals(VocalizeType.WORD, v.exercise.type)
        assertEquals("CASA", v.exercise.content)
    }

    @Test
    fun vocalize_inferredSyllable_whenOnlySyllables() {
        val result = mapper.map(dto(RemoteExerciseType.VoiceRecognition, ExerciseContentDto(syllables = listOf("MA"))))
        val v = assertIs<MappedExercise.Vocalize>(result)
        assertEquals(VocalizeType.SYLLABLE, v.exercise.type)
        assertEquals("MA", v.exercise.content)
    }

    @Test
    fun otherTypes_areUnsupportedForNow() {
        val c = ExerciseContentDto(options = listOf("MA", "PA"), correctOption = "MA")
        val result = mapper.map(dto(RemoteExerciseType.SyllableRecognition, c))
        assertTrue(result is MappedExercise.Unsupported)
    }

    @Test
    fun source_isPreserved_forAttemptSubmission() {
        val d = dto(RemoteExerciseType.VoiceRecognition, ExerciseContentDto(targetWord = "SOL"), order = 7)
        val result = mapper.map(d)
        assertEquals("ex-7", result.source.id)
        assertEquals(1, result.source.contentVersion)
    }
}
