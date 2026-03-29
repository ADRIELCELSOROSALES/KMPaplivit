package com.aplivit.infrastructure

import com.aplivit.core.port.SpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechSynthesisVoice

class IosSpeechSynthesizer : SpeechSynthesizer {
    private val synthesizer = AVSpeechSynthesizer()

    override fun speak(text: String) {
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("es-ES")
        utterance.rate = 0.5f
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun release() {
        stop()
    }
}
