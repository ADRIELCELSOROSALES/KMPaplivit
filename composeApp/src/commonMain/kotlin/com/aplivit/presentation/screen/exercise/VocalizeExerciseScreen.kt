package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.aplivit.core.domain.model.VocalizeExercise
import com.aplivit.core.domain.model.VocalizeType
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.ConnectivityChecker
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
import com.aplivit.presentation.util.LockPortrait
import org.koin.compose.koinInject

@Composable
fun VocalizeExerciseScreen(
    exercise: VocalizeExercise,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    LockPortrait()

    val tts: SpeechSynthesizer = koinInject()
    val recognizer: SpeechRecognizer = koinInject()
    val connectivity: ConnectivityChecker = koinInject()
    val validate: ValidatePronunciationUseCase = koinInject()
    val repo: ProgressRepository = koinInject()

    val vm: VocalizeViewModel = remember {
        VocalizeViewModel(tts, recognizer, connectivity, validate, repo)
    }
    val state by vm.state.collectAsState()

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopListening() }
    }

    // Colores de feedback
    val contentBorderColor = when (state.feedback) {
        VocalizeFeedback.CORRECT -> AppColors.FeedbackCorrect
        VocalizeFeedback.INCORRECT -> AppColors.FeedbackIncorrect
        null -> if (state.isListening) Color(0xFFFFB300) else Color(0xFFBDBDBD)
    }
    val contentBgColor = when (state.feedback) {
        VocalizeFeedback.CORRECT -> AppColors.FeedbackCorrect.copy(alpha = 0.08f)
        VocalizeFeedback.INCORRECT -> AppColors.FeedbackIncorrect.copy(alpha = 0.08f)
        null -> Color.Transparent
    }

    // Tamaño de fuente según el tipo de contenido
    val fontSize = when (exercise.type) {
        VocalizeType.SYLLABLE -> 72.sp
        VocalizeType.WORD -> 56.sp
        VocalizeType.SENTENCE -> 30.sp
    }

    BaseExerciseScreen(
        onMicClick = { vm.toggleListening() },
        onListenClick = { vm.onListenClick() },
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        forwardEnabled = state.isCompleted
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Contenido a vocalizar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, contentBorderColor, RoundedCornerShape(16.dp))
                    .background(contentBgColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = exercise.content,
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A237E),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            // Indicador de escucha
            if (state.isListening) {
                Text(
                    text = "🎙 Escuchando…",
                    fontSize = 18.sp,
                    color = Color(0xFFFFB300),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Mensaje de feedback
            if (state.feedbackMessage.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.feedbackMessage,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (state.feedback) {
                        VocalizeFeedback.CORRECT -> AppColors.FeedbackCorrect
                        VocalizeFeedback.INCORRECT -> AppColors.FeedbackIncorrect
                        null -> Color.Gray
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}
