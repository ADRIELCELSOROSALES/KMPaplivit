package com.aplivit.infrastructure

import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus

class IosSpeechRecognizer(
    private val connectivityChecker: ConnectivityChecker
) : SpeechRecognizer {

    override val mode: RecognitionMode
        get() = if (connectivityChecker.isConnected()) RecognitionMode.STT else RecognitionMode.AMPLITUDE

    private val sfRecognizer = SFSpeechRecognizer(locale = NSLocale("es_ES"))
    private val audioEngine = AVAudioEngine()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null

    init {
        SFSpeechRecognizer.requestAuthorization { _ -> }
        AVAudioSession.sharedInstance().requestRecordPermission { _ -> }
    }

    override fun startListening(expected: String, onResult: (RecognitionResult) -> Unit) {
        if (SFSpeechRecognizer.authorizationStatus() != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
            onResult(RecognitionResult.PermissionDenied)
            return
        }

        if (mode == RecognitionMode.STT) {
            startSttListening(expected, onResult)
        } else {
            onResult(RecognitionResult.SoundDetected)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun startSttListening(expected: String, onResult: (RecognitionResult) -> Unit) {
        // Cancel any previous task and clean up engine before starting fresh
        recognitionTask?.cancel()
        recognitionTask = null
        if (audioEngine.running) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
        }
        recognitionRequest?.endAudio()
        recognitionRequest = null

        try {
            val audioSession = AVAudioSession.sharedInstance()
            // PlayAndRecord avoids conflicting with AVSpeechSynthesizer's session
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
            audioSession.setActive(true, error = null)
        } catch (_: Exception) {}

        var resultDelivered = false

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest().also {
            // true = iOS envía audio al servidor continuamente y es más confiable en audios cortos.
            // Con false, si el clip es muy corto puede devolver result=null sin error → onResult nunca se llama.
            it.shouldReportPartialResults = true
        }

        recognitionTask = sfRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result, error ->
            if (resultDelivered) return@recognitionTaskWithRequest

            if (error != null) {
                resultDelivered = true
                onResult(RecognitionResult.Error)
                return@recognitionTaskWithRequest
            }

            result?.let {
                if (it.isFinal()) {
                    resultDelivered = true
                    val text = it.bestTranscription.formattedString
                    if (text.isBlank()) {
                        onResult(RecognitionResult.NoSound)
                    } else {
                        onResult(RecognitionResult.Transcription(text))
                    }
                }
            }
        }

        val inputNode = audioEngine.inputNode
        val format = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = format) { buffer, _ ->
            recognitionRequest?.appendAudioPCMBuffer(buffer!!)
        }

        audioEngine.prepare()
        audioEngine.startAndReturnError(null)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTapOnBus(0u)
        recognitionRequest?.endAudio()
        recognitionRequest = null
        recognitionTask = null
        try {
            AVAudioSession.sharedInstance().setActive(false, error = null)
        } catch (_: Exception) {}
    }
}
