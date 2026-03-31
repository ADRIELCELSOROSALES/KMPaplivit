package com.aplivit.presentation.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.LevelCard
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    onLevelClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    completed: Boolean = false
) {
    val getLevels: GetLevelsUseCase = koinInject()
    val repo: ProgressRepository = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val vm: HomeViewModel = viewModel { HomeViewModel(getLevels, repo, tts) }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.reload(completedLevel = completed)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "Aplivit",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1565C0),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clickable(onClick = onSettingsClick)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⚙️", fontSize = 28.sp)
            }
        }
        Text(
            text = state.strings.learnToRead,
            fontSize = 16.sp,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.levels) { level ->
                    val isCompleted = level.id in state.progress.completedLevels
                    val isUnlocked = level.id <= state.progress.currentLevel
                    LevelCard(
                        level = level,
                        isUnlocked = isUnlocked,
                        isCompleted = isCompleted,
                        lockedLabel = state.strings.lockedLevel,
                        completedLabel = state.strings.completedLabel,
                        availableLabel = state.strings.availableLabel,
                        onClick = { onLevelClick(level.id) }
                    )
                }
            }
        }
    }
}
