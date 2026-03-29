package com.aplivit.core.domain.usecase

class ValidatePronunciationUseCase {
    operator fun invoke(transcription: String, expected: String): Boolean {
        val normalizedTranscription = transcription.trim().uppercase()
        val normalizedExpected = expected.trim().uppercase()
        return normalizedTranscription.contains(normalizedExpected) ||
               normalizedExpected.contains(normalizedTranscription)
    }
}
