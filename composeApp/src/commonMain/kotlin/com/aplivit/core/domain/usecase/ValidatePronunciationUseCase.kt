package com.aplivit.core.domain.usecase

import com.aplivit.core.port.RecognitionResult

class ValidatePronunciationUseCase {
    fun execute(result: RecognitionResult, expected: String): Boolean = when (result) {
        is RecognitionResult.Transcription -> normalize(result.text).contains(normalize(expected))
        is RecognitionResult.SoundDetected -> true
        is RecognitionResult.NoSound -> false
        is RecognitionResult.Error -> false
        is RecognitionResult.PermissionDenied -> false
    }

    private fun normalize(text: String): String =
        text.trim().uppercase()
            .replace('Á', 'A').replace('É', 'E').replace('Í', 'I')
            .replace('Ó', 'O').replace('Ú', 'U').replace('Ü', 'U')
}
