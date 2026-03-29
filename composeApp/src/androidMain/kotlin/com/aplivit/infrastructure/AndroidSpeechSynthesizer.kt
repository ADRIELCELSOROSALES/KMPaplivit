package com.aplivit.infrastructure

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class AndroidSpeechSynthesizer(context: Context) : SpeechSynthesizer {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                isReady = true
                pendingText?.let { text ->
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    pendingText = null
                }
            }
        }
    }

    override fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            pendingText = text
        }
    }

    override suspend fun speakAndWait(text: String) {
        if (!isReady) {
            pendingText = text
            return
        }
        suspendCancellableCoroutine { cont ->
            val utteranceId = UUID.randomUUID().toString()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {}
                override fun onDone(utteranceId: String) {
                    if (cont.isActive) cont.resume(Unit)
                }
                @Suppress("DEPRECATION")
                override fun onError(utteranceId: String) {
                    if (cont.isActive) cont.resume(Unit)
                }
                override fun onError(utteranceId: String, errorCode: Int) {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
            cont.invokeOnCancellation { tts?.stop() }
        }
    }

    override fun stop() {
        pendingText = null
        tts?.stop()
    }

    override fun release() {
        pendingText = null
        tts?.shutdown()
        tts = null
    }
}
