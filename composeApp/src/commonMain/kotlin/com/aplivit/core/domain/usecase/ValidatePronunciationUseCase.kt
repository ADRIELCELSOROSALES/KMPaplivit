package com.aplivit.core.domain.usecase

import com.aplivit.core.port.RecognitionResult

class ValidatePronunciationUseCase {
    fun execute(result: RecognitionResult, expected: String): Boolean = when (result) {
        is RecognitionResult.Transcription -> {
            val normalizedText = normalize(result.text)
            val normalizedExpected = normalize(expected)
            val threshold = maxOf(1, normalizedExpected.length / 4)
            normalizedText.contains(normalizedExpected) ||
                normalizedText.split(" ").any { word ->
                    levenshtein(word, normalizedExpected) <= threshold
                }
        }
        is RecognitionResult.SoundDetected -> true
        is RecognitionResult.NoSound -> false
        is RecognitionResult.Error -> false
        is RecognitionResult.PermissionDenied -> false
    }

    private fun normalize(text: String): String =
        text.trim().uppercase()
            .replace(" ", "")
            .replace('Á', 'A').replace('É', 'E').replace('Í', 'I')
            .replace('Ó', 'O').replace('Ú', 'U').replace('Ü', 'U')

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
