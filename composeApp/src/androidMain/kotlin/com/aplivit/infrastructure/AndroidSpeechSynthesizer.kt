package com.aplivit.infrastructure

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.port.SpeechSynthesizer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class AndroidSpeechSynthesizer(context: Context) : SpeechSynthesizer {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null

    init {
        Log.d("TTS", "init: creando TextToSpeech")
        tts = TextToSpeech(context) { status ->
            Log.d("TTS", "init callback: status=$status isReady=${status == TextToSpeech.SUCCESS}")
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                isReady = true
                pendingText?.let { text ->
                    Log.d("TTS", "init: reproduciendo pendingText='$text'")
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    pendingText = null
                }
            }
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        val locale = Locale.forLanguageTag(language.ttsLocale)
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("TTS", "setLanguage: idioma ${language.ttsLocale} no soportado, usando default es-ES")
            tts?.setLanguage(Locale("es", "ES"))
        } else {
            Log.d("TTS", "setLanguage: idioma ${language.ttsLocale} configurado correctamente")
        }
    }

    private fun speakWithRate(text: String, rate: Float) {
        if (isReady) {
            tts?.setSpeechRate(rate)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            pendingText = text
        }
    }

    override fun speak(text: String) {
        Log.d("TTS", "speak() isReady=$isReady text='$text'")
        speakWithRate(text, 1.0f)
    }

    override fun speakSyllable(text: String) {
        Log.d("TTS", "speakSyllable() text='$text'")
        speakWithRate(text, 0.5f)
    }

    override fun speakWord(text: String) {
        Log.d("TTS", "speakWord() text='$text'")
        speakWithRate(text, 1.0f)
    }

    override fun speakSentence(text: String) {
        Log.d("TTS", "speakSentence() text='$text'")
        speakWithRate(text, 1.0f)
    }

    override suspend fun speakAndWait(text: String) {
        Log.d("TTS", "speakAndWait() INICIO isReady=$isReady text='$text'")
        if (!isReady) {
            Log.w("TTS", "speakAndWait() TTS no listo, guardando en pendingText")
            pendingText = text
            return
        }
        suspendCancellableCoroutine { cont ->
            val utteranceId = "aplivit_${System.currentTimeMillis()}"
            Log.d("TTS", "speakAndWait() suspendiendo utteranceId=$utteranceId text='$text'")

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    Log.d("TTS", "onStart id=$id  esperado=$utteranceId  match=${id == utteranceId}")
                }

                override fun onDone(id: String?) {
                    Log.d("TTS", "onDone id=$id  esperado=$utteranceId  match=${id == utteranceId}  contActive=${cont.isActive}")
                    if (id != utteranceId) return
                    tts?.setOnUtteranceProgressListener(null)
                    if (cont.isActive) cont.resume(Unit)
                    Log.d("TTS", "speakAndWait() COMPLETO para '$text'")
                }

                @Suppress("DEPRECATION")
                override fun onError(id: String?) {
                    Log.e("TTS", "onError(deprecated) id=$id  esperado=$utteranceId  match=${id == utteranceId}")
                    if (id != utteranceId) return
                    tts?.setOnUtteranceProgressListener(null)
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onError(id: String?, errorCode: Int) {
                    Log.e("TTS", "onError id=$id code=$errorCode  esperado=$utteranceId  match=${id == utteranceId}")
                    if (id != utteranceId) return
                    tts?.setOnUtteranceProgressListener(null)
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onStop(id: String?, interrupted: Boolean) {
                    Log.w("TTS", "onStop id=$id interrupted=$interrupted  esperado=$utteranceId  match=${id == utteranceId}  contActive=${cont.isActive}")
                    if (id != utteranceId) return
                    tts?.setOnUtteranceProgressListener(null)
                    if (cont.isActive) cont.resume(Unit)
                }
            })

            val bundle = Bundle()
            bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, utteranceId)

            cont.invokeOnCancellation {
                Log.w("TTS", "speakAndWait() CANCELADO (scope destruido) utteranceId=$utteranceId text='$text'")
                tts?.stop()
                tts?.setOnUtteranceProgressListener(null)
            }
        }
    }

    override fun stop() {
        Log.w("TTS", "stop() llamado — caller: ${Thread.currentThread().stackTrace[3]}")
        tts?.stop()
    }

    override fun release() {
        Log.d("TTS", "release()")
        pendingText = null
        tts?.shutdown()
        tts = null
    }
}
