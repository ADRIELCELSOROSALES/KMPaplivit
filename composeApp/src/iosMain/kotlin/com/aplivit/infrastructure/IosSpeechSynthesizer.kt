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

    private fun speakWithRate(text: String, rate: Float) {
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(currentLocale)
        utterance.rate = rate
        synthesizer.speakUtterance(utterance)
    }

    override fun speak(text: String) = speakWithRate(text, 0.5f)

    override fun speakSyllable(text: String) = speakWithRate(text, 0.35f)

    override fun speakWord(text: String) = speakWithRate(text, 0.5f)

    override fun speakSentence(text: String) = speakWithRate(text, 0.55f)

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
