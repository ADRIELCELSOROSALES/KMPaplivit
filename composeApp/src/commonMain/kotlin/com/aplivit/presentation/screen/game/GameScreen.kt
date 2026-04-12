package com.aplivit.presentation.screen.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aplivit.core.domain.usecase.CompleteGameUseCase
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.NavigationUseCase
import com.aplivit.core.domain.usecase.UnlockNextLevelUseCase
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.BaseExerciseScreen
import com.aplivit.presentation.util.LockPortrait
import org.koin.compose.koinInject

@Composable
fun GameScreen(
    levelId: Int,
    onCompleted: (nextLevelId: Int) -> Unit,
    onBackNavigate: () -> Unit
) {
    LockPortrait()

    val getLevels: GetLevelsUseCase = koinInject()
    val completeGame: CompleteGameUseCase = koinInject()
    val unlockNext: UnlockNextLevelUseCase = koinInject()
    val validate: ValidatePronunciationUseCase = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val recognizer: SpeechRecognizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val navUseCase: NavigationUseCase = koinInject()

    val vm: GameViewModel = viewModel(key = "game_$levelId") {
        GameViewModel(levelId, getLevels, completeGame, unlockNext, validate, recognizer, tts, repo)
    }
    val state by vm.state.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val level = state.level ?: return

    // Forward is enabled when:
    // 1. The exercise was just completed in this session, OR
    // 2. This level was already completed in a previous session (reviewing)
    val alreadyUnlocked = remember(levelId) { navUseCase.canGoForward(levelId, 1) }
    val forwardEnabled = state.currentStep == GameStep.COMPLETED || alreadyUnlocked

    BaseExerciseScreen(
        onMicClick = {
            if (state.currentStep == GameStep.REPEAT) {
                if (state.isListening) vm.stopListening()
                else vm.startListening(level.word)
            }
        },
        onListenClick = { tts.speak(level.word.lowercase()) },
        onBackClick = onBackNavigate,
        onForwardClick = {
            val (nextLevel, _) = navUseCase.goForward(levelId, 1)
            onCompleted(nextLevel)
        },
        forwardEnabled = forwardEnabled
    ) {
        when (state.currentStep) {
            GameStep.DRAG_DROP -> DragDropGameScreen(
                level = level,
                availableSyllables = state.availableSyllables,
                arrangedSyllables = state.arrangedSyllables,
                feedback = state.feedback,
                strings = state.strings,
                onSyllableMoved = { syllable -> vm.onSyllableMoved(syllable) },
                onReset = { vm.onDragDropReset() },
                onResult = { correct -> vm.onDragDropCompleted(correct) }
            )
            GameStep.SELECTION -> SelectionGameScreen(
                level = level,
                feedback = state.feedback,
                strings = state.strings,
                onResult = { correct -> vm.onSelectionCompleted(correct) }
            )
            GameStep.REPEAT -> RepeatGameScreen(
                level = level,
                isListening = state.isListening,
                feedback = state.feedback,
                strings = state.strings,
                onStopListening = { vm.stopListening() }
            )
            GameStep.COMPLETED -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.strings.levelCompleted,
                        fontSize = 32.sp,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}
