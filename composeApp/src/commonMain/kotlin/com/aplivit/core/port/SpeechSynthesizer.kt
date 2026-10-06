package com.aplivit.core.port

import com.aplivit.core.domain.model.AppLanguage

interface SpeechSynthesizer {
    fun speak(text: String)
    suspend fun speakAndWait(text: String)
    fun speakSyllable(text: String)   // pronunciación lenta y clara (sílaba aislada)
    suspend fun speakSyllableAndWait(text: String)  // sílaba lenta y clara, suspende hasta terminar
    fun speakWord(text: String)       // pronunciación natural de una palabra
    suspend fun speakWordAndWait(text: String)      // palabra natural, suspende hasta terminar
    fun speakSentence(text: String)   // pronunciación natural fluida de una frase
    fun stop()
    fun release()
    suspend fun setLanguage(language: AppLanguage)
}
