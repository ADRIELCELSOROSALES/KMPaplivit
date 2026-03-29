package com.aplivit.core.port

enum class RecognitionMode { STT, AMPLITUDE }

sealed class RecognitionResult {
    data class Transcription(val text: String) : RecognitionResult()
    object SoundDetected : RecognitionResult()
    object NoSound : RecognitionResult()
    object Error : RecognitionResult()
    object PermissionDenied : RecognitionResult()
}

interface SpeechRecognizer {
    val mode: RecognitionMode
    fun startListening(expected: String, onResult: (RecognitionResult) -> Unit)
    fun stopListening()
}
