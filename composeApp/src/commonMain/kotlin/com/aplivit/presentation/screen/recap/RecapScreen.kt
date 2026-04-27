package com.aplivit.presentation.screen.recap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.BaseExerciseScreen
import com.aplivit.presentation.component.SyllableCard
import com.aplivit.presentation.util.LockPortrait
import com.aplivit.shared.stringsFor
import org.koin.compose.koinInject

@Composable
fun RecapScreen(
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    LockPortrait()

    val getLevels: GetLevelsUseCase = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: RecapViewModel = remember { RecapViewModel(getLevels, repo, tts) }
    val state by vm.state.collectAsState()
    val strings = stringsFor(repo.getSelectedLanguage())

    BaseExerciseScreen(
        onMicClick = {},
        onListenClick = { vm.speakInstruction() },
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        forwardEnabled = true   // repaso: siempre se puede avanzar
    ) {
        if (state.isLoading) {
            CircularProgressIndicator()
            return@BaseExerciseScreen
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.recapTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = strings.tapSyllableHint,
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            if (state.syllables.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "—",
                        fontSize = 40.sp,
                        color = Color(0xFFBDBDBD)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.syllables) { syllable ->
                        SyllableCard(
                            text = syllable,
                            onClick = { vm.speakSyllable(syllable) }
                        )
                    }
                }
            }
        }
    }
}
