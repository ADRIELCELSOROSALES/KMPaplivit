package com.aplivit.infrastructure

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizerApi
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AndroidSpeechRecognizer(
    private val context: Context,
    private val connectivityChecker: ConnectivityChecker
) : SpeechRecognizer {

    override val mode: RecognitionMode
        get() = if (connectivityChecker.isConnected()) RecognitionMode.STT else RecognitionMode.AMPLITUDE

    private var recognizer: AndroidSpeechRecognizerApi? = null
    private var amplitudeJob: Job? = null

    override fun startListening(expected: String, onResult: (RecognitionResult) -> Unit) {
        if (mode == RecognitionMode.STT) {
            startSttListening(expected, onResult)
        } else {
            startAmplitudeListening(onResult)
        }
    }

    private fun startSttListening(expected: String, onResult: (RecognitionResult) -> Unit) {
        recognizer = AndroidSpeechRecognizerApi.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(AndroidSpeechRecognizerApi.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                onResult(RecognitionResult.Transcription(text))
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
    }

    private fun startAmplitudeListening(onResult: (RecognitionResult) -> Unit) {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        amplitudeJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ShortArray(bufferSize)
            audioRecord.startRecording()
            var soundDetected = false
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 3000) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                if (read > 0) {
                    val amplitude = buffer.take(read).maxOrNull()?.toInt()?.and(0xFFFF) ?: 0
                    if (amplitude > 5000) {
                        soundDetected = true
                        break
                    }
                }
                delay(100)
            }
            audioRecord.stop()
            audioRecord.release()
            if (soundDetected) onResult(RecognitionResult.SoundDetected)
            else onResult(RecognitionResult.NoSound)
        }
    }

    override fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        amplitudeJob?.cancel()
        amplitudeJob = null
    }
}
