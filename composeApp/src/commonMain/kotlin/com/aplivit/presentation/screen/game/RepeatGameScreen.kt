package com.aplivit.presentation.screen.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AudioButton
import org.koin.compose.koinInject

@Composable
fun RepeatGameScreen(
    level: Level,
    recognitionMode: RecognitionMode,
    isListening: Boolean,
    feedback: String,
    onStartListening: (String) -> Unit,
    onStopListening: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()

    LaunchedEffect(Unit) {
        tts.speak("Repite: ${level.word}")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Juego 3: Repite la palabra", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (recognitionMode == RecognitionMode.STT) "Modo: Reconocimiento de voz"
                   else "Modo: Detección de sonido",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = level.word,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1565C0)
        )
        Spacer(Modifier.height(8.dp))
        AudioButton(
            label = "▶ Escuchar",
            onClick = { tts.speak(level.word) }
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = if (isListening) "Escuchando..." else "Toca para hablar",
            fontSize = 16.sp,
            color = if (isListening) Color(0xFFF44336) else Color.Black
        )
        Spacer(Modifier.height(16.dp))
        AudioButton(
            label = if (isListening) "⏹ Detener" else "🎤 Hablar",
            isActive = isListening,
            onClick = {
                if (isListening) onStopListening()
                else onStartListening(level.word)
            }
        )

        if (feedback.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(feedback, color = Color.Red, fontSize = 16.sp)
        }
    }
}
