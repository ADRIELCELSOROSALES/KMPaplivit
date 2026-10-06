package com.aplivit.offline

import com.aplivit.infrastructure.remote.dto.ExerciseContentDto
import com.aplivit.infrastructure.remote.dto.RemoteExerciseType
import com.aplivit.infrastructure.remote.dto.SyllablePairDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Paridad con docs/RF-12-contrato-validacion.md: el veredicto offline debe coincidir con el que
 * daría el backend (Exercise.EvaluateAnswer). Cada caso replica un ejemplo de esa doc.
 */
class OfflineAnswerValidatorTest {

    private fun eval(type: RemoteExerciseType, content: ExerciseContentDto, answer: String) =
        OfflineAnswerValidator.evaluate(type, content, answer)

    @Test
    fun syllableRecognition_ignoreCase_ordinal() {
        val c = ExerciseContentDto(options = listOf("MA", "PA"), correctOption = "MA")
        assertEquals(true, eval(RemoteExerciseType.SyllableRecognition, c, "ma"))
        assertEquals(false, eval(RemoteExerciseType.SyllableRecognition, c, "PA"))
        assertEquals(false, eval(RemoteExerciseType.SyllableRecognition, c, ""))
    }

    @Test
    fun syllableMatching_isSymmetric_andHandlesMalformed() {
        val c = ExerciseContentDto(pairs = listOf(SyllablePairDto("LE", "PA")))
        assertEquals(true, eval(RemoteExerciseType.SyllableMatching, c, "PA|LE"))  // simétrico
        assertEquals(true, eval(RemoteExerciseType.SyllableMatching, c, "le|pa"))
        assertEquals(false, eval(RemoteExerciseType.SyllableMatching, c, "LE|LE"))
        assertEquals(false, eval(RemoteExerciseType.SyllableMatching, c, "LE"))     // sin pipe
        assertEquals(false, eval(RemoteExerciseType.SyllableMatching, c, "|LE"))    // mitad vacía
        assertEquals(false, eval(RemoteExerciseType.SyllableMatching, ExerciseContentDto(), "A|B"))
    }

    @Test
    fun wordBuilding_comparesFinalWord_notSyllables() {
        val c = ExerciseContentDto(syllables = listOf("LE", "MA"), targetWord = "LEMA")
        assertEquals(true, eval(RemoteExerciseType.WordBuilding, c, "lema"))
        assertEquals(false, eval(RemoteExerciseType.WordBuilding, c, "LE"))
    }

    @Test
    fun wordCompletion_comparesSyllable_notFullWord() {
        val c = ExerciseContentDto(wordWithGap = "CA_A", correctSyllable = "S", options = listOf("S", "T"))
        assertEquals(true, eval(RemoteExerciseType.WordCompletion, c, "S"))
        assertEquals(false, eval(RemoteExerciseType.WordCompletion, c, "CASA"))
    }

    @Test
    fun voiceRecognition_isNotAutoEvaluated() {
        val c = ExerciseContentDto(targetWord = "MAMA")
        assertNull(eval(RemoteExerciseType.VoiceRecognition, c, "MAMA"))
        assertTrue(!OfflineAnswerValidator.isAutoEvaluable(RemoteExerciseType.VoiceRecognition))
        assertTrue(OfflineAnswerValidator.isAutoEvaluable(RemoteExerciseType.SyllableRecognition))
    }

    @Test
    fun noTrim_trailingSpaceMatters() {
        // El backend NO hace trim al evaluar: un espacio de más cuenta como respuesta distinta.
        val c = ExerciseContentDto(options = listOf("MA"), correctOption = "MA")
        assertEquals(false, eval(RemoteExerciseType.SyllableRecognition, c, "MA "))
    }
}
