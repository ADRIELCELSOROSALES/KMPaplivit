package com.aplivit.core.domain.usecase

import com.aplivit.core.port.RecognitionResult

private const val SENTENCE_MATCH_THRESHOLD = 0.8f

class ValidatePronunciationUseCase {

    /**
     * Valida el resultado del reconocimiento contra el texto esperado.
     * - Para palabras/sílabas (sin espacios): compara con Levenshtein.
     * - Para oraciones (con espacios): compara palabra por palabra,
     *   requiere ≥ 80 % de palabras reconocidas correctamente.
     */
    fun execute(result: RecognitionResult, expected: String): Boolean = when (result) {
        is RecognitionResult.Transcription -> {
            if (expected.trim().contains(' ')) {
                validateSentence(result.text, expected)
            } else {
                validateWord(result.text, expected)
            }
        }
        is RecognitionResult.SoundDetected -> true
        is RecognitionResult.NoSound -> false
        is RecognitionResult.Error -> false
        is RecognitionResult.PermissionDenied -> false
    }

    // ── Validación de palabra o sílaba ────────────────────────────────────────

    private fun validateWord(transcription: String, expected: String): Boolean {
        val normalized = normalizeFlat(transcription)
        val normalizedExpected = normalizeFlat(expected)
        val threshold = maxOf(1, normalizedExpected.length / 4)
        return normalized.contains(normalizedExpected) ||
            normalized.split(" ").any { word ->
                levenshtein(word, normalizedExpected) <= threshold
            }
    }

    // ── Validación de oración (palabra por palabra) ───────────────────────────

    private fun validateSentence(transcription: String, expected: String): Boolean {
        val transcriptionWords = normalizeWords(transcription)
        val expectedWords = normalizeWords(expected)
        if (expectedWords.isEmpty()) return false

        val matchCount = expectedWords.count { expectedWord ->
            transcriptionWords.any { transcribedWord ->
                val threshold = maxOf(1, expectedWord.length / 4)
                levenshtein(transcribedWord, expectedWord) <= threshold
            }
        }
        return matchCount.toFloat() / expectedWords.size >= SENTENCE_MATCH_THRESHOLD
    }

    // ── Normalización ─────────────────────────────────────────────────────────

    /** Para sílabas/palabras: elimina espacios y aplana acentos. */
    private fun normalizeFlat(text: String): String =
        text.trim().uppercase()
            .replace(" ", "")
            .stripAccents()

    /** Para oraciones: separa en palabras, aplana acentos, quita puntuación. */
    private fun normalizeWords(text: String): List<String> =
        text.trim().uppercase()
            .stripAccents()
            .split("\\s+".toRegex())
            .map { word -> word.filter { it.isLetter() } }
            .filter { it.isNotBlank() }

    private fun String.stripAccents(): String =
        this.replace('Á', 'A').replace('É', 'E').replace('Í', 'I')
            .replace('Ó', 'O').replace('Ú', 'U').replace('Ü', 'U')
            .replace('À', 'A').replace('È', 'E').replace('Ì', 'I')
            .replace('Ò', 'O').replace('Ù', 'U')
            .replace('Â', 'A').replace('Ê', 'E').replace('Î', 'I')
            .replace('Ô', 'O').replace('Û', 'U')
            .replace('Ñ', 'N')

    // ── Distancia de Levenshtein ──────────────────────────────────────────────

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
        return dp[a.length][b.length]
    }
}
