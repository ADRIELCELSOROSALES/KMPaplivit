package com.aplivit.core.port

import com.aplivit.core.domain.model.AppLanguage

interface SpeechSynthesizer {
    fun speak(text: String)
    suspend fun speakAndWait(text: String)
    fun stop()
    fun release()
    suspend fun setLanguage(language: AppLanguage)
}
