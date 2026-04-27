package com.aplivit.presentation.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.SessionResumeUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import org.koin.compose.koinInject

@Composable
fun HomeScreen(
    onLevelClick: (Int) -> Unit,
    completed: Boolean = false
) {
    val getLevels: GetLevelsUseCase = koinInject()
    val repo: ProgressRepository = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val sessionResume: SessionResumeUseCase = koinInject()
    val vm: HomeViewModel = remember { HomeViewModel(getLevels, repo, tts, sessionResume) }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.reload(completedLevel = completed)
    }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            onLevelClick(state.progress.currentLevel)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Aplivit",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1565C0),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
