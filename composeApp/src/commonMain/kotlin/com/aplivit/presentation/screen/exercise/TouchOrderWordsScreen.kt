package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.aplivit.core.domain.model.SentenceExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TouchOrderWordsScreen(
    exercise: SentenceExercise,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: TouchOrderWordsViewModel = remember { TouchOrderWordsViewModel(tts, repo) }
    val state by vm.state.collectAsState()

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
    }

    // Oración armada hasta ahora
    val assembledSentence = state.selectedOrder.joinToString(" ") { displayIdx ->
        exercise.words[exercise.shuffledIndices[displayIdx]]
    }

    BaseExerciseScreen(
        onMicClick = {},
        onListenClick = { vm.playTarget() },
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        forwardEnabled = state.isCompleted
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ordena las palabras",
                fontSize = 20.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(24.dp))

            // Caja que muestra la oración armada progresivamente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFBDBDBD), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = assembledSentence.ifEmpty { "..." },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (assembledSentence.isEmpty()) Color(0xFFBDBDBD) else Color(0xFF1A237E),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(40.dp))

            // Palabras desordenadas
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                exercise.shuffledIndices.forEachIndexed { displayIndex, _ ->
                    val flash = state.flashStates[displayIndex]
                    val alreadySelected = displayIndex in state.selectedOrder

                    val bgColor = when (flash) {
                        FlashState.CORRECT -> AppColors.FeedbackCorrect
                        FlashState.INCORRECT -> AppColors.FeedbackIncorrect
                        null -> if (alreadySelected) AppColors.FeedbackCorrect else Color(0xFF4CAF50)
                    }
                    val enabled = !alreadySelected && flash == null && !state.isCompleted
                    val word = exercise.words[exercise.shuffledIndices[displayIndex]]

                    WordChip(
                        text = word,
                        backgroundColor = bgColor,
                        enabled = enabled,
                        onClick = { vm.onWordTapped(displayIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordChip(
    text: String,
    backgroundColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
