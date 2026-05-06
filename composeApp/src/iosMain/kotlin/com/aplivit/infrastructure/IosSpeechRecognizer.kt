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
    private var audioSessionActivated = false
    private var tapInstalled = false

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
        // If the system interrupted the audio session, the engine may have stopped but the tap
        // remains installed — always remove it if we installed one, otherwise installTapOnBus
        // on the next attempt fails silently and the recognizer hangs forever.
        if (audioEngine.running) audioEngine.stop()
        if (tapInstalled) {
            tapInstalled = false
            try { audioEngine.inputNode.removeTapOnBus(0u) } catch (_: Exception) {}
        }
        recognitionRequest?.endAudio()
        recognitionRequest = null

        try {
            val audioSession = AVAudioSession.sharedInstance()
            // PlayAndRecord avoids conflicting with AVSpeechSynthesizer's session
            audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
            audioSession.setActive(true, error = null)
            audioSessionActivated = true
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
        tapInstalled = true

        audioEngine.prepare()
        val engineStarted = audioEngine.startAndReturnError(null)
        if (!engineStarted) {
            onResult(RecognitionResult.Error)
            return
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stopListening() {
        if (audioEngine.running) audioEngine.stop()
        // Only access inputNode if we installed a tap — accessing it unconditionally activates
        // the microphone hardware on iOS and reroutes audio from speaker to earpiece.
        if (tapInstalled) {
            tapInstalled = false
            try { audioEngine.inputNode.removeTapOnBus(0u) } catch (_: Exception) {}
        }
        recognitionRequest?.endAudio()
        recognitionRequest = null
        recognitionTask = null
        if (audioSessionActivated) {
            audioSessionActivated = false
            try { AVAudioSession.sharedInstance().setActive(false, error = null) } catch (_: Exception) {}
        }
    }
}
