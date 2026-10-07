package com.aplivit.infrastructure

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.RecognitionResult
import com.aplivit.core.port.SpeechRecognizer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.math.abs

@OptIn(ExperimentalForeignApi::class)
class IosSpeechRecognizer(
    private val connectivityChecker: ConnectivityChecker
) : SpeechRecognizer {

    override val mode: RecognitionMode
        get() = if (connectivityChecker.isConnected()) RecognitionMode.STT else RecognitionMode.AMPLITUDE

    private var sfRecognizer: SFSpeechRecognizer? = null
    private val audioEngine = AVAudioEngine()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var audioSessionActivated = false
    private var tapInstalled = false

    init {
        SFSpeechRecognizer.requestAuthorization { _ -> }
        AVAudioSession.sharedInstance().requestRecordPermission { _ -> }
    }

    override fun startListening(expected: String, language: AppLanguage, onResult: (RecognitionResult) -> Unit) {
        val speechAuthorized = SFSpeechRecognizer.authorizationStatus() ==
            SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
        // El permiso de microfono es DISTINTO del de reconocimiento de voz: sin este, el motor de
        // audio arranca igual pero captura silencio, y la pantalla se queda en "escuchando" para
        // siempre sin que nadie reporte nada.
        val micGranted = AVAudioSession.sharedInstance().recordPermission() == AVAudioSessionRecordPermissionGranted

        println("STT [startListening] modo=$mode speech=$speechAuthorized mic=$micGranted")

        if (!speechAuthorized || !micGranted) {
            // Se vuelven a pedir: si el alumno todavia no respondio el dialogo, la proxima vez ya
            // hay respuesta. Si los denegó, iOS no muestra nada y hay que ir a Ajustes.
            SFSpeechRecognizer.requestAuthorization { _ -> }
            AVAudioSession.sharedInstance().requestRecordPermission { _ -> }
            onResult(RecognitionResult.PermissionDenied)
            return
        }

        if (mode == RecognitionMode.STT) {
            sfRecognizer = SFSpeechRecognizer(locale = NSLocale(language.ttsLocale))
            startSttListening(onResult)
        } else {
            startAmplitudeListening(onResult)
        }
    }

    private fun startSttListening(onResult: (RecognitionResult) -> Unit) {
        resetAudioEngine()
        if (!activateRecordingSession()) {
            onResult(RecognitionResult.Error)
            return
        }

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
                println("STT [task] error=${error.localizedDescription}")
                onResult(RecognitionResult.Error)
                return@recognitionTaskWithRequest
            }

            result?.let {
                if (it.isFinal()) {
                    resultDelivered = true
                    val text = it.bestTranscription.formattedString
                    println("STT [task] final='$text'")
                    if (text.isBlank()) {
                        onResult(RecognitionResult.NoSound)
                    } else {
                        onResult(RecognitionResult.Transcription(text))
                    }
                }
            }
        }

        installTap { buffer -> recognitionRequest?.appendAudioPCMBuffer(buffer) }

        audioEngine.prepare()
        if (!audioEngine.startAndReturnError(null)) {
            println("STT [engine] no arranco")
            onResult(RecognitionResult.Error)
        }
    }

    /**
     * Modo sin conexión: se escucha de verdad y solo se aprueba si hubo sonido, con el mismo
     * criterio que Android (ver AmplitudeSpeechRecognizer: ventana de 3s y umbral de amplitud).
     * Antes esta rama devolvía SoundDetected al instante sin abrir siquiera el micrófono, con lo
     * que el ejercicio se daba por resuelto sin que el alumno dijera nada.
     */
    private fun startAmplitudeListening(onResult: (RecognitionResult) -> Unit) {
        resetAudioEngine()
        if (!activateRecordingSession()) {
            onResult(RecognitionResult.Error)
            return
        }

        var resultDelivered = false
        fun deliver(result: RecognitionResult) {
            if (resultDelivered) return
            resultDelivered = true
            println("STT [amplitud] $result")
            stopListening()
            onResult(result)
        }

        installTap { buffer ->
            val channels = buffer.floatChannelData
            val samples = channels?.get(0)
            if (samples != null) {
                var peak = 0f
                for (i in 0 until buffer.frameLength.toInt()) {
                    val value = abs(samples[i])
                    if (value > peak) peak = value
                }
                if (peak > SOUND_THRESHOLD) deliver(RecognitionResult.SoundDetected)
            }
        }

        audioEngine.prepare()
        if (!audioEngine.startAndReturnError(null)) {
            println("STT [engine] no arranco")
            deliver(RecognitionResult.Error)
            return
        }

        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, LISTEN_WINDOW_NANOS),
            dispatch_get_main_queue()
        ) { deliver(RecognitionResult.NoSound) }
    }

    private fun installTap(onBuffer: (platform.AVFAudio.AVAudioPCMBuffer) -> Unit) {
        val inputNode = audioEngine.inputNode
        val format = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = format) { buffer, _ ->
            buffer?.let(onBuffer)
        }
        tapInstalled = true
    }

    /**
     * PlayAndRecord con DefaultToSpeaker: sin esa opción, mientras el micrófono está abierto iOS
     * saca el audio por el auricular en vez del altavoz y el TTS parece mudo (es el botón de
     * escuchar la palabra, que no se oía durante el ejercicio).
     */
    private fun activateRecordingSession(): Boolean {
        val session = AVAudioSession.sharedInstance()
        val categorySet = session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            withOptions = AVAudioSessionCategoryOptionDefaultToSpeaker,
            error = null
        )
        val activated = session.setActive(true, error = null)
        audioSessionActivated = activated
        if (!categorySet || !activated) {
            println("STT [sesion] categoria=$categorySet activa=$activated")
        }
        return activated
    }

    private fun resetAudioEngine() {
        recognitionTask?.cancel()
        recognitionTask = null
        // Si el sistema interrumpió la sesión, el engine puede haber parado con el tap todavía
        // puesto: instalar otro encima falla en silencio y el reconocedor queda colgado.
        if (audioEngine.running) audioEngine.stop()
        removeTap()
        recognitionRequest?.endAudio()
        recognitionRequest = null
    }

    private fun removeTap() {
        if (!tapInstalled) return
        tapInstalled = false
        try {
            audioEngine.inputNode.removeTapOnBus(0u)
        } catch (_: Exception) {
        }
    }

    override fun stopListening() {
        if (audioEngine.running) audioEngine.stop()
        // Solo se toca inputNode si habíamos instalado un tap: accederlo sin más enciende el
        // hardware del micrófono y redirige el audio del altavoz al auricular.
        removeTap()
        recognitionRequest?.endAudio()
        recognitionRequest = null
        recognitionTask = null

        if (audioSessionActivated) {
            audioSessionActivated = false
            // Se vuelve a Playback en vez de desactivar la sesión: así el TTS que viene después
            // (feedback, instrucción del nivel siguiente) suena por el altavoz sin pelearse con
            // la sesión de grabación.
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        }
    }

    private companion object {
        /** Pico normalizado a partir del cual se considera que el alumno dijo algo. */
        const val SOUND_THRESHOLD = 0.15f

        /** Ventana de escucha del modo amplitud, igual que en Android (3 segundos). */
        const val LISTEN_WINDOW_NANOS = 3_000_000_000L
    }
}
