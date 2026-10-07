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

    /** Generación del último parcial recibido, para saber si el alumno dejó de hablar. */
    private var partialGeneration = 0

    /** Identifica cada escucha, para que el tope de una no corte la siguiente. */
    private var listenAttempt = 0

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
        var heardSomething = false

        fun deliver(result: RecognitionResult) {
            if (resultDelivered) return
            resultDelivered = true
            println("STT [resultado] $result")
            stopListening()
            onResult(result)
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest().also {
            // Imprescindible: con audio continuo, iOS solo marca la transcripción como final
            // cuando se corta el audio. Los parciales son la única señal de que el alumno habló.
            it.shouldReportPartialResults = true
        }

        recognitionTask = sfRecognizer?.recognitionTaskWithRequest(recognitionRequest!!) { result, error ->
            if (resultDelivered) return@recognitionTaskWithRequest

            if (error != null) {
                // Al cerrar el audio sin voz, iOS reporta un error ("no speech detected"). Para el
                // alumno eso no es una falla de la app: simplemente no se escuchó nada.
                println("STT [task] error=${error.localizedDescription}")
                deliver(if (heardSomething) RecognitionResult.Error else RecognitionResult.NoSound)
                return@recognitionTaskWithRequest
            }

            val transcription = result?.bestTranscription?.formattedString.orEmpty()
            if (result != null && result.isFinal()) {
                println("STT [task] final='$transcription'")
                deliver(
                    if (transcription.isBlank()) RecognitionResult.NoSound
                    else RecognitionResult.Transcription(transcription)
                )
                return@recognitionTaskWithRequest
            }

            if (transcription.isNotBlank()) {
                heardSomething = true
                // El alumno sigue hablando: se reinicia la cuenta de silencio.
                scheduleSilenceCheck(transcription)
            }
        }

        installTap { buffer -> recognitionRequest?.appendAudioPCMBuffer(buffer) }

        audioEngine.prepare()
        if (!audioEngine.startAndReturnError(null)) {
            println("STT [engine] no arranco")
            deliver(RecognitionResult.Error)
            return
        }

        // Tope duro: si no llega nada, se cierra el audio igual para que el ejercicio no quede
        // colgado en "escuchando" hasta que el alumno toque un botón.
        val attempt = ++listenAttempt
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, MAX_LISTEN_NANOS), dispatch_get_main_queue()) {
            if (attempt == listenAttempt && !resultDelivered) {
                println("STT [tope] se corta el audio tras la ventana maxima")
                recognitionRequest?.endAudio()
            }
        }
    }

    /**
     * iOS no corta solo la escucha (a diferencia del reconocedor de Android, que detecta el fin
     * del habla): si no se cierra el audio, la transcripción nunca llega a ser final y la pantalla
     * se queda en "escuchando" para siempre. Acá se cierra sola tras una pausa del alumno.
     */
    private fun scheduleSilenceCheck(lastTranscription: String) {
        val generation = ++partialGeneration
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, SILENCE_NANOS), dispatch_get_main_queue()) {
            if (generation == partialGeneration) {
                println("STT [silencio] pausa tras '$lastTranscription': se cierra el audio")
                recognitionRequest?.endAudio()
            }
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

        /** Pausa del alumno que se toma como "ya terminó de hablar". */
        const val SILENCE_NANOS = 1_500_000_000L

        /** Tope de escucha: pasado esto se cierra el audio aunque no se haya oído nada. */
        const val MAX_LISTEN_NANOS = 8_000_000_000L
    }
}
