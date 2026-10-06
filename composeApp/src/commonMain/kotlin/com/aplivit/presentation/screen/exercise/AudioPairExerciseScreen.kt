package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.AudioPairExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
import com.aplivit.presentation.component.SalientText
import com.aplivit.presentation.component.SyllableCard
import com.aplivit.presentation.util.rememberIsLandscape
import org.koin.compose.koinInject

@Composable
fun AudioPairExerciseScreen(
    exercise: AudioPairExercise,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: AudioPairViewModel = remember { AudioPairViewModel(tts, repo) }
    val state by vm.state.collectAsState()

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
    }

    val isLandscape = rememberIsLandscape()
    val outerPadding = if (isLandscape) 16.dp else 24.dp

    BaseExerciseScreen(
        onMicClick = {},
        onListenClick = { vm.playTarget() },
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        forwardEnabled = state.isCompleted
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(outerPadding)) {
            SyllableCard(
                text = exercise.target,
                backgroundColor = AppColors.Outline,
                textColor = AppColors.InkDark,
                salientEnabled = false,
                onClick = { vm.playTarget() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                exercise.options.forEachIndexed { index, option ->
                    AudioPairOptionCard(
                        option = option,
                        flash = state.flashState[index],
                        isFound = index in state.foundCorrect,
                        salienceEnabled = exercise.useSalience,
                        onClick = { vm.onOptionTapped(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPairOptionCard(
    option: String,
    flash: FlashState?,
    isFound: Boolean,
    salienceEnabled: Boolean,
    onClick: () -> Unit
) {
    val textColor = when {
        flash == FlashState.CORRECT -> AppColors.FeedbackCorrect
        flash == FlashState.INCORRECT -> AppColors.FeedbackIncorrect
        isFound -> AppColors.FeedbackCorrect
        else -> AppColors.InkDark
    }
    val enabled = !isFound && flash == null
    val fontSize = when {
        option.length <= 3 -> 36.sp
        option.length <= 6 -> 28.sp
        option.length <= 9 -> 20.sp
        else -> 16.sp
    }
    SalientText(
        onClick = onClick,
        salientEnabled = salienceEnabled && enabled
    ) {
        Text(
            text = option,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
