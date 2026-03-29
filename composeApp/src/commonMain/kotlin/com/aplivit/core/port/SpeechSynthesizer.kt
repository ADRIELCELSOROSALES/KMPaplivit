package com.aplivit.core.port

interface SpeechSynthesizer {
    fun speak(text: String)
    suspend fun speakAndWait(text: String)
    fun stop()
    fun release()
}
