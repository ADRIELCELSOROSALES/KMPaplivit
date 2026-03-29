package com.aplivit.presentation.screen.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aplivit.core.domain.usecase.CompleteGameUseCase
import com.aplivit.core.domain.usecase.GetLevelsUseCase
import com.aplivit.core.domain.usecase.UnlockNextLevelUseCase
import com.aplivit.core.domain.usecase.ValidatePronunciationUseCase
import com.aplivit.core.port.SpeechRecognizer
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.util.LockPortrait
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun GameScreen(levelId: Int, onCompleted: () -> Unit) {
    LockPortrait()

    val getLevels: GetLevelsUseCase = koinInject()
    val completeGame: CompleteGameUseCase = koinInject()
    val unlockNext: UnlockNextLevelUseCase = koinInject()
    val validate: ValidatePronunciationUseCase = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val recognizer: SpeechRecognizer = koinInject()

    val vm: GameViewModel = viewModel(key = "game_$levelId") {
        GameViewModel(levelId, getLevels, completeGame, unlockNext, validate, recognizer, tts)
    }
    val state by vm.state.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val level = state.level ?: return

    when (state.currentStep) {
        GameStep.DRAG_DROP -> DragDropGameScreen(
            level = level,
            availableSyllables = state.availableSyllables,
            arrangedSyllables = state.arrangedSyllables,
            feedback = state.feedback,
            onSyllableMoved = { syllable -> vm.onSyllableMoved(syllable) },
            onReset = { vm.onDragDropReset() },
            onResult = { correct -> vm.onDragDropCompleted(correct) }
        )
        GameStep.SELECTION -> SelectionGameScreen(
            level = level,
            feedback = state.feedback,
            onResult = { correct -> vm.onSelectionCompleted(correct) }
        )
        GameStep.REPEAT -> RepeatGameScreen(
            level = level,
            recognitionMode = state.recognitionMode,
            isListening = state.isListening,
            feedback = state.feedback,
            onStartListening = { expected -> vm.startListening(expected) },
            onStopListening = { vm.stopListening() }
        )
        GameStep.COMPLETED -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("¡Nivel completado!", fontSize = 32.sp, color = Color(0xFF4CAF50))
            }
            LaunchedEffect(Unit) {
                delay(2000)
                onCompleted()
            }
        }
    }
}
