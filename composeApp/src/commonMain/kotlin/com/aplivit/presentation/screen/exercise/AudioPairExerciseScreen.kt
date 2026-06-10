package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aplivit.core.domain.model.AudioPairExercise
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
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
    val rowSpacing = if (isLandscape) 16.dp else 24.dp

    BaseExerciseScreen(
        onMicClick = {},
        onListenClick = { vm.playTarget() },
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        forwardEnabled = state.isCompleted
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(outerPadding),
            verticalArrangement = Arrangement.spacedBy(rowSpacing, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            exercise.options.chunked(2).forEachIndexed { rowIdx, rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowOptions.forEachIndexed { colIdx, option ->
                        val index = rowIdx * 2 + colIdx
                        AudioPairOptionCard(
                            option = option,
                            index = index,
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
}

@Composable
private fun AudioPairOptionCard(
    option: String,
    index: Int,
    flash: FlashState?,
    isFound: Boolean,
    salienceEnabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        flash == FlashState.CORRECT -> AppColors.FeedbackCorrect
        flash == FlashState.INCORRECT -> AppColors.FeedbackIncorrect
        isFound -> AppColors.FeedbackCorrect
        else -> AppColors.BgWhite
    }
    val textColor = if (backgroundColor == AppColors.BgWhite) AppColors.InkDark else AppColors.BgWhite
    val enabled = !isFound && flash == null
    SyllableCard(
        text = option,
        backgroundColor = backgroundColor,
        textColor = textColor,
        salientEnabled = salienceEnabled && enabled,
        onClick = onClick
    )
}
