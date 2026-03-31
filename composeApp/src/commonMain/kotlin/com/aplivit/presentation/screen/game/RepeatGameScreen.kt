package com.aplivit.presentation.screen.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level
import com.aplivit.core.port.RecognitionMode
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AudioButton
import com.aplivit.shared.AppStrings
import org.koin.compose.koinInject

@Composable
fun RepeatGameScreen(
    level: Level,
    recognitionMode: RecognitionMode,
    isListening: Boolean,
    feedback: String?,
    strings: AppStrings,
    onStartListening: (String) -> Unit,
    onStopListening: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()

    DisposableEffect(Unit) {
        onDispose { onStopListening() }
    }

    LaunchedEffect(Unit) {
        tts.speak("${strings.speak}: ${level.word}")
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Juego 3: Repite la palabra",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = if (recognitionMode == RecognitionMode.STT) "Modo: Reconocimiento de voz"
                   else "Modo: Detección de sonido",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = level.word,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1565C0),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        AudioButton(
            icon = "👂",
            label = strings.speak,
            onClick = { tts.speak(level.word) }
        )
        Text(
            text = if (isListening) strings.listening else strings.speak,
            fontSize = 16.sp,
            color = if (isListening) Color(0xFFF44336) else Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        AudioButton(
            icon = if (isListening) "⏹" else "🎤",
            label = if (isListening) strings.stop else strings.speak,
            isActive = isListening,
            onClick = {
                if (isListening) onStopListening()
                else onStartListening(level.word)
            }
        )

        if (feedback != null) {
            Text(
                text = feedback,
                color = Color.Red,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    }
}
