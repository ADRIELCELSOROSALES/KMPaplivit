package com.aplivit.infrastructure

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.port.SpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechSynthesisVoice

class IosSpeechSynthesizer : SpeechSynthesizer {
    private val synthesizer = AVSpeechSynthesizer()
    private var currentLocale: String = AppLanguage.SPANISH.ttsLocale

    override suspend fun setLanguage(language: AppLanguage) {
        currentLocale = language.ttsLocale
    }

    override fun speak(text: String) {
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(currentLocale)
        utterance.rate = 0.5f
        synthesizer.speakUtterance(utterance)
    }

    override suspend fun speakAndWait(text: String) {
        speak(text)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun release() {
        stop()
    }
}
