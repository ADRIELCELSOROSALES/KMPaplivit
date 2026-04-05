package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aplivit.core.domain.model.TouchExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
import com.aplivit.presentation.component.WordWithSyllables
import org.koin.compose.koinInject

@Composable
fun TouchSyllableInWordScreen(
    exercise: TouchExercise,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: TouchViewModel = viewModel { TouchViewModel(tts, repo) }
    val state by vm.state.collectAsState()

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
    }

    val syllableColors = exercise.options.indices.associate { index ->
        val color = when (state.flashState[index]) {
            FlashState.CORRECT -> AppColors.FeedbackCorrect
            FlashState.INCORRECT -> AppColors.FeedbackIncorrect
            null -> if (index in state.foundCorrect) AppColors.FeedbackCorrect else Color(0xFF4CAF50)
        }
        index to color
    }

    val enabledIndices = exercise.options.indices
        .filter { it !in state.foundCorrect && !state.flashState.containsKey(it) }
        .toSet()

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
                text = "Toca la sílaba:",
                fontSize = 20.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = exercise.target,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E)
            )
            Spacer(Modifier.height(48.dp))
            WordWithSyllables(
                word = exercise.options.joinToString("-"),
                syllables = exercise.options,
                onSyllableTap = { vm.onOptionTapped(it) },
                syllableColors = syllableColors,
                enabledIndices = enabledIndices
            )
        }
    }
}
