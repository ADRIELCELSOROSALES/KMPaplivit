package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.LinkExercise
import com.aplivit.core.domain.model.LinkItem
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
import com.aplivit.presentation.component.SalientText
import com.aplivit.presentation.component.LinkCanvas
import org.koin.compose.koinInject

@Composable
fun LinkExerciseScreen(
    exercise: LinkExercise,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: LinkViewModel = remember { LinkViewModel(tts, repo) }
    val state by vm.state.collectAsState()

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
    }

    // Posiciones de anclaje de cada ítem: izquierda → borde derecho-centro
    //                                     derecha   → borde izquierdo-centro
    val leftOffsets = remember { mutableStateMapOf<Int, Offset>() }
    val rightOffsets = remember { mutableStateMapOf<Int, Offset>() }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BaseExerciseScreen(
        onMicClick = {},
        onListenClick = { vm.playTarget() },
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        forwardEnabled = state.isCompleted
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onGloballyPositioned { rootCoords = it }
        ) {
            // Líneas debajo de los ítems (z-order inferior)
            LinkCanvas(
                confirmedPairs = state.confirmedPairs,
                flashPair = state.flashPair,
                flashCorrect = state.flashCorrect,
                leftOffsets = leftOffsets,
                rightOffsets = rightOffsets,
                modifier = Modifier.fillMaxSize()
            )

            // Dos columnas de ítems
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Columna izquierda
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    exercise.leftItems.forEachIndexed { i, item ->
                        val confirmed = i in state.confirmedPairs
                        val selected = state.selectedLeft == i
                        LinkItemCard(
                            item = item,
                            selected = selected,
                            confirmed = confirmed,
                            onClick = { vm.onLeftTapped(i) },
                            salientEnabled = exercise.useSalience,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                rootCoords?.let { root ->
                                    leftOffsets[i] = root.localPositionOf(
                                        sourceCoordinates = coords,
                                        relativeToSource = Offset(
                                            x = coords.size.width.toFloat(),
                                            y = coords.size.height / 2f
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                // Espacio central donde pasan las líneas (sin contenido)
                Box(modifier = Modifier.weight(0.3f))

                // Columna derecha
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    exercise.rightItems.forEachIndexed { i, item ->
                        val confirmed = state.confirmedPairs.values.contains(i)
                        LinkItemCard(
                            item = item,
                            selected = false,
                            confirmed = confirmed,
                            onClick = { vm.onRightTapped(i) },
                            salientEnabled = exercise.useSalience,
                            modifier = Modifier.onGloballyPositioned { coords ->
                                rootCoords?.let { root ->
                                    rightOffsets[i] = root.localPositionOf(
                                        sourceCoordinates = coords,
                                        relativeToSource = Offset(
                                            x = 0f,
                                            y = coords.size.height / 2f
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkItemCard(
    item: LinkItem,
    selected: Boolean,
    confirmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    salientEnabled: Boolean = true
) {
    val bgColor = when {
        confirmed -> AppColors.FeedbackCorrect
        selected -> Color(0xFFE3F2FD)
        else -> Color.White
    }
    val borderColor = when {
        confirmed -> AppColors.FeedbackCorrect
        selected -> AppColors.FeedbackCorrect
        else -> Color(0xFFBDBDBD)
    }
    val textColor = if (confirmed) Color.White else Color(0xFF1A237E)

    SalientText(
        onClick = onClick,
        modifier = modifier,
        salientEnabled = salientEnabled && !confirmed
    ) {
        Box(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(bgColor, RoundedCornerShape(12.dp))
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (item.text != null) {
                Text(
                    text = item.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            } else {
                // Placeholder para imágenes (imageRes)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🖼", fontSize = 24.sp)
                }
            }
        }
    }
}
