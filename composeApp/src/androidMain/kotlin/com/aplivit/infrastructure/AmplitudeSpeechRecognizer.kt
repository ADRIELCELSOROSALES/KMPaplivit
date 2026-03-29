package com.aplivit.infrastructure

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
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
        amplitudeJob?.cancel()
        amplitudeJob = null
    }
}
