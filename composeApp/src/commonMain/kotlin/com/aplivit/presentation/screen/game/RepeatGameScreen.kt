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
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.AppStrings
import org.koin.compose.koinInject

@Composable
fun RepeatGameScreen(
    level: Level,
    isListening: Boolean,
    feedback: String?,
    strings: AppStrings,
    onStopListening: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()

    DisposableEffect(Unit) {
        onDispose { onStopListening() }
    }

    LaunchedEffect(Unit) {
        tts.speak(level.word)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = level.word,
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1565C0),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (isListening) {
            Text(
                text = strings.listening,
                fontSize = 18.sp,
                color = Color(0xFFF44336),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
        }
        if (feedback != null) {
            Text(
                text = feedback,
                color = Color.Red,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }
    }
}
