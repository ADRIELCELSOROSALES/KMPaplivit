package com.aplivit.infrastructure

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizerApi
import com.aplivit.AppContext
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer

class AndroidSpeechRecognizer(private val context: Context) : SpeechRecognizer {

    override val mode = RecognitionMode.STT

    private var recognizer: AndroidSpeechRecognizerApi? = null

    override fun startListening(expected: String, onResult: (RecognitionResult) -> Unit) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            val requestPermission = AppContext.requestMicPermission
            if (requestPermission != null) {
                requestPermission { isGranted ->
                    if (isGranted) startListeningInternal(expected, onResult)
                    else onResult(RecognitionResult.PermissionDenied)
                }
            } else {
                onResult(RecognitionResult.PermissionDenied)
            }
            return
        }
        startListeningInternal(expected, onResult)
    }

    private fun startListeningInternal(expected: String, onResult: (RecognitionResult) -> Unit) {
        try {
            recognizer = AndroidSpeechRecognizerApi.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val candidates = results
                        ?.getStringArrayList(AndroidSpeechRecognizerApi.RESULTS_RECOGNITION)
                        ?: emptyList<String>()
                    val normalizedExpected = expected.trim().uppercase().replace(" ", "")
                    // Prefer the candidate that contains or most closely matches the expected word
                    val best = candidates.maxByOrNull { candidate ->
                        val n = candidate.trim().uppercase().replace(" ", "")
                        when {
                            n.contains(normalizedExpected) -> 2
                            normalizedExpected.contains(n) -> 1
                            else -> 0
                        }
                    } ?: candidates.firstOrNull() ?: ""
                    onResult(RecognitionResult.Transcription(best))
                }
                override fun onError(error: Int) { onResult(RecognitionResult.Error) }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            onResult(RecognitionResult.Error)
        }
    }

    override fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }
}
