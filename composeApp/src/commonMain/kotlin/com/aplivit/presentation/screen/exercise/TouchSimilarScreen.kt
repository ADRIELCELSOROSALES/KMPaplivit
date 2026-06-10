package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.aplivit.core.domain.model.TouchExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
import com.aplivit.presentation.component.SalientText
import com.aplivit.presentation.util.rememberIsLandscape
import org.koin.compose.koinInject

@Composable
fun TouchSimilarScreen(
    exercise: TouchExercise,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: TouchViewModel = remember { TouchViewModel(tts, repo) }
    val state by vm.state.collectAsState()

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
    }

    val isLandscape = rememberIsLandscape()
    val gridColumns = if (isLandscape) 5 else 3
    val itemFontSize = if (isLandscape) 32.sp else 48.sp

    BaseExerciseScreen(
        onMicClick = {},
        onListenClick = { vm.playTarget() },
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        forwardEnabled = state.isCompleted
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) 12.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 24.dp)
        ) {
            itemsIndexed(exercise.options) { index, option ->
                val flash = state.flashState[index]
                val textColor = when (flash) {
                    FlashState.CORRECT -> AppColors.FeedbackCorrect
                    FlashState.INCORRECT -> AppColors.FeedbackIncorrect
                    null -> if (index in state.foundCorrect) AppColors.FeedbackCorrect else Color.Black
                }
                val enabled = index !in state.foundCorrect && !state.flashState.containsKey(index)
                SalientText(
                    onClick = { vm.onOptionTapped(index) },
                    salientEnabled = exercise.useSalience && enabled
                ) {
                    Text(
                        text = option,
                        fontSize = itemFontSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
