package com.aplivit.infrastructure

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosSpeechSynthesizer : SpeechSynthesizer {
    private val synthDelegate = SynthesizerDelegate()
    private val synthesizer = AVSpeechSynthesizer().also {
        it.delegate = synthDelegate
        // Que el sintetizador gestione SU PROPIA sesión de audio en vez de la de la app: por
        // defecto usa la de la app (usesApplicationAudioSession=true) y si esa sesión no quedó
        // bien activada, el TTS queda mudo. Con false reproduce por su cuenta, de forma confiable.
        it.usesApplicationAudioSession = false
    }
    private var currentLocale: String = AppLanguage.SPANISH.ttsLocale

    override suspend fun setLanguage(language: AppLanguage) {
        currentLocale = language.ttsLocale
    }

    // Sin una AVAudioSession en categoría .playback el TTS no suena: la sesión por defecto respeta
    // el switch de silencio del iPhone (y queda inactiva tras un dictado, que la pone en pausa). Se
    // reconfigura antes de cada reproducción para volver a la salida por altavoz aunque el teléfono
    // esté en silencio y para reactivar la sesión luego de escuchar por micrófono.
    private fun ensurePlaybackSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    private fun speakWithRate(text: String, rate: Float) {
        ensurePlaybackSession()
        synthesizer.stopSpeakingAtBoundary(platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance(string = text)
        // Solo asignar voz si iOS tiene una para ese locale; con voz nula algunas versiones no
        // emiten audio. Nula -> el sistema usa la voz por defecto del idioma de la UI.
        AVSpeechSynthesisVoice.voiceWithLanguage(currentLocale)?.let { utterance.voice = it }
        utterance.rate = rate
        synthesizer.speakUtterance(utterance)
    }

    override fun speak(text: String) = speakWithRate(text, 0.4f)
    // Lowercase prevents TTS from reading uppercase syllables as Roman numerals (e.g. "LI" → 51)
    override fun speakSyllable(text: String) = speakWithRate(text.lowercase(), 0.3f)
    // Lowercase prevents all-caps words/syllables from being read as acronyms or Roman numerals
    override fun speakWord(text: String) = speakWithRate(text.lowercase(), 0.4f)
    override fun speakSentence(text: String) = speakWithRate(text, 0.4f)

    override suspend fun speakSyllableAndWait(text: String) =
        // Lowercase: evita que el TTS lea sílabas en mayúscula como números romanos.
        speakAndWaitWithRate(text.lowercase(), 0.3f)

    override suspend fun speakWordAndWait(text: String) =
        speakAndWaitWithRate(text.lowercase(), 0.4f)

    override suspend fun speakAndWait(text: String) = speakAndWaitWithRate(text, 0.4f)

    private suspend fun speakAndWaitWithRate(text: String, rate: Float) {
        suspendCancellableCoroutine { cont ->
            synthDelegate.onFinish = { cont.resume(Unit) }
            speakWithRate(text, rate)
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
