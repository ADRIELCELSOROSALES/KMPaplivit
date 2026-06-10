package com.aplivit.core.port

import com.aplivit.core.domain.model.AppLanguage

interface SpeechSynthesizer {
    fun speak(text: String)
    suspend fun speakAndWait(text: String)
    fun speakSyllable(text: String)   // pronunciación lenta y clara (sílaba aislada)
    fun speakWord(text: String)       // pronunciación natural de una palabra
    fun speakSentence(text: String)   // pronunciación natural fluida de una frase
    fun stop()
    fun release()
    suspend fun setLanguage(language: AppLanguage)
}
