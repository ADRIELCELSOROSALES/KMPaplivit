package com.aplivit.infrastructure

import android.content.Context
import android.speech.tts.TextToSpeech
import com.aplivit.core.port.SpeechSynthesizer
import java.util.Locale

class AndroidSpeechSynthesizer(context: Context) : SpeechSynthesizer {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                isReady = true
            }
        }
    }

    override fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun release() {
        tts?.shutdown()
        tts = null
    }
}
