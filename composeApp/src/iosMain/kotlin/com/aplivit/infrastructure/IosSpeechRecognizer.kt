package com.aplivit.infrastructure

import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
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

    init {
        // Solicitar permisos al inicio para que iOS muestre los diálogos oportunamente
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
        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryRecord, error = null)
            audioSession.setActive(true, error = null)
        } catch (_: Exception) {
            // Continuar aunque falle la configuración de sesión
        }

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

    @OptIn(ExperimentalForeignApi::class)
    override fun stopListening() {
        audioEngine.stop()
        audioEngine.inputNode.removeTapOnBus(0u)
        recognitionRequest?.endAudio()
        recognitionRequest = null
        try {
            AVAudioSession.sharedInstance().setActive(false, error = null)
        } catch (_: Exception) {}
    }
}
