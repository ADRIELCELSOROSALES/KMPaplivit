package com.aplivit.core.port

interface SpeechSynthesizer {
    fun speak(text: String)
    fun stop()
    fun release()
}
