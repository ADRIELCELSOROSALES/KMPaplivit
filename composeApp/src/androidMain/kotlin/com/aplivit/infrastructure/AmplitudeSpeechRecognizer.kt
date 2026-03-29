package com.aplivit.infrastructure

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.aplivit.AppContext
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AmplitudeSpeechRecognizer(private val context: Context) : SpeechRecognizer {

    override val mode = RecognitionMode.AMPLITUDE

    private var amplitudeJob: Job? = null

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
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onResult(RecognitionResult.Error)
            return
        }
        amplitudeJob = CoroutineScope(Dispatchers.IO).launch {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            try {
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
                if (soundDetected) onResult(RecognitionResult.SoundDetected)
                else onResult(RecognitionResult.NoSound)
            } catch (e: Exception) {
                onResult(RecognitionResult.Error)
            } finally {
                audioRecord.release()
            }
        }
    }

    override fun stopListening() {
        amplitudeJob?.cancel()
        amplitudeJob = null
    }
}
