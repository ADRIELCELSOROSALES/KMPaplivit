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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level
import com.aplivit.presentation.component.AppColors
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.util.rememberIsLandscape
import com.aplivit.shared.AppStrings
import org.koin.compose.koinInject

@Composable
fun RepeatGameScreen(
    level: Level,
    isListening: Boolean,
    feedback: String?,
    strings: AppStrings,
    onStopListening: () -> Unit,
    onStartListening: () -> Unit = {}
) {
    val tts: SpeechSynthesizer = koinInject()

    DisposableEffect(Unit) {
        onDispose { onStopListening() }
    }

    LaunchedEffect(Unit) {
        tts.speakAndWait(level.word.lowercase())
        onStartListening()
    }

    val isLandscape = rememberIsLandscape()
    val wordFontSize = when {
        level.word.length <= 8  -> if (isLandscape) 40.sp else 64.sp
        level.word.length <= 14 -> if (isLandscape) 32.sp else 48.sp
        level.word.length <= 20 -> if (isLandscape) 24.sp else 36.sp
        else                    -> 28.sp
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = level.word,
            fontSize = wordFontSize,
            fontWeight = FontWeight.ExtraBold,
            color = AppColors.InkDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (isListening) {
            Text(
                text = strings.listening,
                fontSize = 18.sp,
                color = AppColors.FeedbackIncorrect,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
        }
        if (feedback != null) {
            Text(
                text = feedback,
                color = AppColors.FeedbackIncorrect,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
        }
    }
}
