package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.aplivit.core.domain.model.TouchExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
import org.koin.compose.koinInject

@Composable
fun TouchOrderSyllablesScreen(
    exercise: TouchExercise,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: TouchOrderViewModel = remember { TouchOrderViewModel(tts, repo) }
    val state by vm.state.collectAsState()

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
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
            // Ranuras de progreso: muestra las sílabas correctas ya tocadas
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                exercise.correctIndices.forEachIndexed { position, correctIdx ->
                    val filledIndex = state.selectedOrder.getOrNull(position)
                    val syllable = if (filledIndex != null) exercise.options[filledIndex] else ""
                    ProgressSlot(syllable = syllable, filled = filledIndex != null)
                }
            }

            Spacer(Modifier.height(48.dp))

            // Sílabas desordenadas para tocar
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                exercise.options.forEachIndexed { index, syllable ->
                    val flash = state.flashStates[index]
                    val alreadySelected = index in state.selectedOrder

                    val bgColor = when (flash) {
                        FlashState.CORRECT -> AppColors.FeedbackCorrect
                        FlashState.INCORRECT -> AppColors.FeedbackIncorrect
                        null -> if (alreadySelected) AppColors.FeedbackCorrect else Color(0xFF4CAF50)
                    }
                    val enabled = !alreadySelected && flash == null && !state.isCompleted

                    SyllableChip(
                        text = syllable,
                        backgroundColor = bgColor,
                        enabled = enabled,
                        onClick = { vm.onSyllableTapped(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressSlot(syllable: String, filled: Boolean) {
    Box(
        modifier = Modifier
            .shadow(if (filled) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            .background(
                if (filled) AppColors.FeedbackCorrect else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (filled) 0.dp else 2.dp,
                color = if (filled) Color.Transparent else Color(0xFFBDBDBD),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (filled) syllable else "  ",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun SyllableChip(
    text: String,
    backgroundColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
