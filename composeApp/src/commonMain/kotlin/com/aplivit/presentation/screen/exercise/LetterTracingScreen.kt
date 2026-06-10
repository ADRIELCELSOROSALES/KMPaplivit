package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.aplivit.infrastructure.content.LetterPaths
import com.aplivit.presentation.component.BaseExerciseScreen
import com.aplivit.presentation.component.LetterTracingAnimation
import org.koin.compose.koinInject

/**
 * Screen that displays an animated letter/syllable tracing.
 *
 * The user observes the animation and then practices writing on paper.
 * - 👂  replays the TTS pronunciation and the tracing animation
 * - 🎤  no-op (no digital writing input for this exercise)
 * - ▶   enabled once the first animation completes
 *
 * @param letter       The letter or syllable to trace (e.g. "A", "MA").
 * @param onBackClick  Navigate back.
 * @param onForwardClick Navigate forward to next exercise.
 */
@Composable
fun LetterTracingScreen(
    letter: String,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    viewModel: LetterTracingViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(letter) {
        viewModel.loadLetter(letter)
    }

    // Build strokes once per letter change — paths are stateless and reusable
    val strokes = remember(letter) {
        if (letter.length == 1) {
            LetterPaths.getStrokesForLetter(letter[0])
        } else {
            // For syllables (e.g. "MA"), combine paths of each character
            // laid out side by side by shifting x coordinates
            buildSyllableStrokes(letter)
        }
    }

    BaseExerciseScreen(
        onMicClick = { /* no digital input */ },
        onListenClick = { viewModel.replay() },
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        forwardEnabled = state.isCompleted
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            LetterTracingAnimation(
                strokes = strokes,
                playKey = state.playKey,
                onAnimationComplete = { viewModel.onAnimationComplete() },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(200f / 250f)
            )
        }
    }
}

/**
 * Combines per-character strokes into a side-by-side syllable layout.
 * Each character occupies a 200-wide slot; total viewport width = 200 * charCount.
 * The LetterTracingAnimation viewportWidth must be updated accordingly.
 *
 * For simplicity this function returns a flattened list using a shared
 * 200x250 viewport with characters scaled to ~100 wide and centered.
 */
private fun buildSyllableStrokes(syllable: String): List<Path> {
    val result = mutableListOf<Path>()
    val charCount = syllable.length.coerceAtLeast(1)
    val slotWidth = 200f / charCount  // each char occupies this many viewport units

    syllable.forEachIndexed { index, char ->
        val offsetX = index * slotWidth
        val scaleX = slotWidth / 200f
        val charStrokes = LetterPaths.getStrokesForLetter(char)
        for (stroke in charStrokes) {
            val scaled = Path()
            scaled.addPath(stroke)
            // Scale x to fit slot width, then shift into the correct slot position
            val matrix = Matrix()
            matrix.scale(scaleX, 1f)
            matrix.translate(offsetX / scaleX, 0f)
            scaled.transform(matrix)
            result.add(scaled)
        }
    }
    return result
}
