package com.aplivit.infrastructure

import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.AVFAudio.AVAudioEngine

class IosSpeechRecognizer(
    private val connectivityChecker: ConnectivityChecker
) : SpeechRecognizer {

    override val mode: RecognitionMode
        get() = if (connectivityChecker.isConnected()) RecognitionMode.STT else RecognitionMode.AMPLITUDE

    private val sfRecognizer = SFSpeechRecognizer(locale = platform.Foundation.NSLocale("es_ES"))
    private val audioEngine = AVAudioEngine()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null

    override fun startListening(expected: String, onResult: (RecognitionResult) -> Unit) {
        if (mode == RecognitionMode.STT) {
            startSttListening(expected, onResult)
        } else {
            onResult(RecognitionResult.SoundDetected)
        }
    }

    private fun startSttListening(expected: String, onResult: (RecognitionResult) -> Unit) {
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        val inputNode = audioEngine.inputNode
        recognitionRequest?.shouldReportPartialResults = false

        sfRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result, error ->
            if (error != null) {
                onResult(RecognitionResult.Error)
                return@recognitionTaskWithRequest
            }
            result?.let {
                if (it.isFinal()) {
                    val text = it.bestTranscription.formattedString
                    onResult(RecognitionResult.Transcription(text))
                }
            }
        }

        val format = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = format) { buffer, _ ->
            recognitionRequest?.appendAudioPCMBuffer(buffer!!)
        }

        audioEngine.prepare()
        audioEngine.startAndReturnError(null)
    }

    override fun stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTapOnBus(0u)
        recognitionRequest?.endAudio()
        recognitionRequest = null
    }
}
