package com.aplivit.presentation.screen.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.SyllableCard
import org.koin.compose.koinInject

@Composable
fun LevelScreen(levelId: Int, onStartGames: () -> Unit, onBack: () -> Unit) {
    val getLevels: GetLevelsUseCase = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val vm: LevelViewModel = viewModel(key = "level_$levelId") { LevelViewModel(levelId, getLevels, tts) }
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }
        val level = state.level ?: return@Column

        Text("Nivel ${level.id}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
        Spacer(Modifier.height(8.dp))
        Text(level.word, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1565C0))
        Spacer(Modifier.height(24.dp))
        Text("Toca cada sílaba para escucharla", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            level.syllables.forEach { syllable ->
                SyllableCard(text = syllable.text, onClick = { vm.speakSyllable(syllable.text) })
            }
        }
        Spacer(Modifier.height(40.dp))
        Button(onClick = onStartGames) {
            Text("Comenzar juegos", fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) {
            Text("Volver", fontSize = 16.sp)
        }
    }
}
