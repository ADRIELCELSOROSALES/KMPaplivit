package com.aplivit.presentation.screen.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.aplivit.presentation.component.AppColors
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.SyllableCard
import com.aplivit.presentation.util.rememberIsLandscape
import org.koin.compose.koinInject

@Composable
fun LevelScreen(levelId: Int, onStartGames: () -> Unit, onBack: () -> Unit) {
    val getLevels: GetLevelsUseCase = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: LevelViewModel = remember(levelId) { LevelViewModel(levelId, getLevels, tts, repo) }
    val state by vm.state.collectAsState()
    val isLandscape = rememberIsLandscape()

    if (state.isLoading) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val level = state.level ?: return
    val strings = state.strings

    if (isLandscape) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: word info
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Nivel ${level.id}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.InkDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = level.word,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.InkDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    level.syllables.forEach { syllable ->
                        SyllableCard(text = syllable.text, onClick = { vm.speakSyllable(syllable.text) })
                    }
                }
            }
            // Right: actions
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Button(onClick = onStartGames, modifier = Modifier.fillMaxWidth(0.8f)) {
                    Text(strings.startGames, fontSize = 16.sp)
                }
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth(0.8f)) {
                    Text(strings.back, fontSize = 14.sp)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Nivel ${level.id}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.InkDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = level.word,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.InkDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                level.syllables.forEach { syllable ->
                    SyllableCard(text = syllable.text, onClick = { vm.speakSyllable(syllable.text) })
                }
            }
            Button(onClick = onStartGames, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(strings.startGames, fontSize = 18.sp)
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text(strings.back, fontSize = 16.sp)
            }
        }
    }
}
