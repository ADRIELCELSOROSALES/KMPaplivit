package com.aplivit.presentation.screen.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.AudioPairExercise
import com.aplivit.core.domain.model.Level
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.SyllableCard
import com.aplivit.presentation.screen.exercise.AudioPairViewModel
import com.aplivit.presentation.screen.exercise.FlashState
import com.aplivit.presentation.util.rememberIsLandscape
import com.aplivit.shared.AppStrings
import org.koin.compose.koinInject

private val DISTRACTOR_POOL = listOf(
    "MA", "ME", "MI", "PA", "PE", "PI",
    "SA", "SE", "SI", "LA", "LE", "LI",
    "CA", "CO", "CU", "TA", "TE", "TI",
    "HO", "PO", "GA", "BO", "DO", "NI",
    "RO", "LU", "NO", "LO", "GO", "NU"
)

@Composable
fun AudioPairGameScreen(
    level: Level,
    feedback: String?,
    strings: AppStrings,
    onCompleted: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: AudioPairViewModel = remember(level.id) { AudioPairViewModel(tts, repo) }
    val state by vm.state.collectAsState()

    val exercise = remember(level.id) { buildExercise(level) }

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
    }

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) onCompleted()
    }

    val isLandscape = rememberIsLandscape()
    val rowSpacing = if (isLandscape) 16.dp else 24.dp

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
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
                            flash = state.flashState[index],
                            isFound = index in state.foundCorrect,
                            salienceEnabled = exercise.useSalience,
                            onClick = { vm.onOptionTapped(index) }
                        )
                    }
                }
            }
        }

        if (feedback != null) {
            Text(
                text = feedback,
                color = AppColors.FeedbackIncorrect,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
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

private fun buildExercise(level: Level): AudioPairExercise {
    val targetUpper = level.syllables.firstOrNull()?.text?.uppercase() ?: "MA"
    val variant = titleCase(targetUpper)
    val distractors = DISTRACTOR_POOL
        .filter { !it.equals(targetUpper, ignoreCase = true) }
        .shuffled()
        .take(2)
    val options = (listOf(targetUpper, variant) + distractors).shuffled()
    return AudioPairExercise(
        id = level.id,
        target = targetUpper,
        options = options
    )
}

private fun titleCase(text: String): String =
    if (text.length <= 1) text.lowercase()
    else text.first().uppercase() + text.drop(1).lowercase()
