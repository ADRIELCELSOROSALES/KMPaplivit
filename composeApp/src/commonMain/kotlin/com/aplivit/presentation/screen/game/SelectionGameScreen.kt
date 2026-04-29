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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.SyllableCard
import com.aplivit.shared.AppStrings
import org.koin.compose.koinInject

private val ALL_SYLLABLES = listOf(
    "MA", "ME", "MI", "PA", "PE", "PI",
    "SA", "SE", "SI", "LA", "LE", "LI",
    "CA", "CO", "CU", "TA", "TE", "TI",
    "HO", "PO", "GA", "BO", "DO", "NI",
    "RO", "LU", "NO", "LO", "GO", "NU",
    "BE", "JO", "GRA", "CIAS"
)

private val ALL_WORDS = listOf(
    "BUENOS", "DIAS", "BUENAS", "NOCHES", "TARDES",
    "MUCHAS", "GRACIAS", "NADA", "FAVOR", "COMO",
    "ESTAS", "BIEN", "LUEGO", "HASTA", "PERMISO",
    "SIENTO", "MUCHO", "GUSTO", "TAL", "ENTIENDO",
    "POR", "CON", "LO", "QUE", "DE", "ESTOY",
    "PROVECHO", "BUEN", "IGUALMENTE", "DISCULPA"
)

@Composable
fun SelectionGameScreen(
    level: Level,
    feedback: String?,
    strings: AppStrings,
    onResult: (Boolean) -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    var currentSyllableIndex by remember { mutableIntStateOf(0) }
    val targetSyllable = level.syllables.getOrNull(currentSyllableIndex)?.text ?: ""
    val isPhrase = level.word.contains(" ")

    val options = remember(targetSyllable) {
        val pool = if (isPhrase) ALL_WORDS else ALL_SYLLABLES
        val distractors = pool
            .filter { it != targetSyllable }
            .shuffled()
            .take(2)
        (distractors + targetSyllable).shuffled()
    }

    LaunchedEffect(targetSyllable) {
        if (targetSyllable.isNotEmpty()) tts.speakSyllable(targetSyllable)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SyllableCard(
            text = "▶",
            onClick = { tts.speakSyllable(targetSyllable) },
            backgroundColor = Color(0xFF2196F3)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                SyllableCard(
                    text = option,
                    onClick = {
                        val correct = option == targetSyllable
                        if (correct && currentSyllableIndex < level.syllables.size - 1) {
                            // Move to next syllable in this level
                        }
                        onResult(correct)
                    },
                    backgroundColor = Color(0xFF4CAF50)
                )
            }
        }

        if (feedback != null) {
            Text(
                text = feedback,
                color = Color.Red,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    }
}
