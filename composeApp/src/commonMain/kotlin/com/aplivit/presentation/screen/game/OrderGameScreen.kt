package com.aplivit.presentation.screen.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.WordWithSyllables
import com.aplivit.shared.AppStrings
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Juego de orden: el mensaje vocal pide tocar las sílabas de la palabra en el orden correcto.
 * Por cada toque el adulto oye el sonido de la sílaba y, visualmente, ésta se agranda (resalta)
 * y vuelve a su tamaño anterior (efecto de salience, provisto por [WordWithSyllables]/SalientText).
 *
 * - Tocar la sílaba esperada la marca como completada (verde) y avanza al siguiente índice.
 * - Re-tocar una sílaba ya completada sólo repite su sonido.
 * - Tocar una sílaba fuera de orden reinicia la secuencia y reporta error.
 * Al completar todas en orden, se reporta éxito.
 */
@Composable
fun OrderGameScreen(
    level: Level,
    feedback: String?,
    strings: AppStrings,
    onResult: (Boolean) -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val scope = rememberCoroutineScope()
    val syllables = level.syllables.map { it.text }

    var nextIndex by remember(level.id) { mutableIntStateOf(0) }
    val completed = remember(level.id) { mutableStateListOf<Int>() }

    // Al entrar, se escucha la palabra completa como referencia antes de tocar las sílabas.
    LaunchedEffect(level.id) {
        tts.speakWord(level.word)
    }

    val syllableColors = completed.associateWith { AppColors.FeedbackCorrect.copy(alpha = 0.25f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WordWithSyllables(
            word = level.word,
            syllables = syllables,
            syllableColors = syllableColors,
            onSyllableTap = { index ->
                // El resaltado (agrandar/volver) lo aplica SalientText en cada toque.
                when {
                    index == nextIndex -> {
                        completed.add(index)
                        nextIndex++
                        if (nextIndex >= syllables.size) {
                            // Última sílaba: escuchar la sílaba, luego la palabra completa,
                            // y recién entonces avanzar (evita que el "muy bien" corte el audio).
                            scope.launch {
                                tts.speakSyllableAndWait(syllables[index])
                                tts.speakWordAndWait(level.word)
                                onResult(true)
                            }
                        } else {
                            tts.speakSyllable(syllables[index])
                        }
                    }
                    index < nextIndex -> {
                        // Sílaba ya completada: sólo repite el sonido, sin penalizar.
                        tts.speakSyllable(syllables[index])
                    }
                    else -> {
                        // Fuera de orden: reproduce el sonido, reinicia la secuencia y reporta error.
                        tts.speakSyllable(syllables[index])
                        completed.clear()
                        nextIndex = 0
                        onResult(false)
                    }
                }
            }
        )

        if (feedback != null) {
            Text(
                text = feedback,
                color = AppColors.FeedbackIncorrect,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    }
}
