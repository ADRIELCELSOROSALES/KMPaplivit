package com.aplivit.infrastructure

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject
import kotlin.coroutines.resume

class IosSpeechSynthesizer : SpeechSynthesizer {
    private val synthDelegate = SynthesizerDelegate()
    private val synthesizer = AVSpeechSynthesizer().also { it.delegate = synthDelegate }
    private var currentLocale: String = AppLanguage.SPANISH.ttsLocale

    override suspend fun setLanguage(language: AppLanguage) {
        currentLocale = language.ttsLocale
    }

    private fun speakWithRate(text: String, rate: Float) {
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(currentLocale)
        utterance.rate = rate
        synthesizer.speakUtterance(utterance)
    }

    override fun speak(text: String) = speakWithRate(text, 0.4f)
    // Lowercase prevents TTS from reading uppercase syllables as Roman numerals (e.g. "LI" → 51)
    override fun speakSyllable(text: String) = speakWithRate(text.lowercase(), 0.3f)
    // Lowercase prevents all-caps words/syllables from being read as acronyms or Roman numerals
    override fun speakWord(text: String) = speakWithRate(text.lowercase(), 0.4f)
    override fun speakSentence(text: String) = speakWithRate(text, 0.4f)

    override suspend fun speakAndWait(text: String) {
        suspendCancellableCoroutine { cont ->
            synthDelegate.onFinish = { cont.resume(Unit) }
            speakWithRate(text, 0.4f)
            cont.invokeOnCancellation {
                synthDelegate.onFinish = null
                stop()
            }
        }
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }

    override fun release() {
        stop()
    }
}

private class SynthesizerDelegate : NSObject(), AVSpeechSynthesizerDelegateProtocol {
    var onFinish: (() -> Unit)? = null

    override fun speechSynthesizer(
        synthesizer: AVSpeechSynthesizer,
        didFinishSpeechUtterance: AVSpeechUtterance
    ) {
        val cb = onFinish
        onFinish = null
        cb?.invoke()
    }
}
