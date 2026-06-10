package com.aplivit.presentation.screen.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.model.LinkPair
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.LinkCanvas
import com.aplivit.shared.AppStrings
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

private val ALL_SYLLABLES = listOf(
    "MA", "ME", "MI", "PA", "PE", "PI",
    "SA", "SE", "SI", "LA", "LE", "LI",
    "CA", "CO", "CU", "TA", "TE", "TI",
    "HO", "PO", "GA", "BO", "DO", "NI",
    "RO", "LU", "NO", "LO", "GO", "NU"
)

private val ALL_WORDS = listOf(
    "BUENOS", "DIAS", "BUENAS", "NOCHES", "TARDES",
    "MUCHAS", "GRACIAS", "NADA", "FAVOR", "COMO",
    "ESTAS", "BIEN", "LUEGO", "HASTA", "PERMISO",
    "SIENTO", "MUCHO", "GUSTO", "TAL", "ENTIENDO",
    "POR", "CON", "LO", "QUE", "DE", "ESTOY",
    "PROVECHO", "BUEN", "IGUALMENTE", "DISCULPA"
)

private data class LinkGameData(
    val leftItems: List<String>,
    val rightItems: List<String>,
    val target: String,
    val correctLeftIdx: Int,
    val correctRightIdx: Int
)

@Composable
fun LinkGameScreen(
    level: Level,
    feedback: String?,
    strings: AppStrings,
    onResult: (Boolean) -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val data = remember(level.id) { buildLinkData(level) }

    val leftBoxes = remember(level.id) { mutableStateMapOf<Int, Rect>() }
    val rightBoxes = remember(level.id) { mutableStateMapOf<Int, Rect>() }
    val leftAnchors = remember(level.id) { mutableStateMapOf<Int, Offset>() }
    val rightAnchors = remember(level.id) { mutableStateMapOf<Int, Offset>() }

    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dragSource by remember(level.id) { mutableStateOf<Int?>(null) }
    var dragCurrent by remember(level.id) { mutableStateOf<Offset?>(null) }
    var confirmedPair by remember(level.id) { mutableStateOf<LinkPair?>(null) }
    var flashPair by remember(level.id) { mutableStateOf<LinkPair?>(null) }
    var flashCorrect by remember(level.id) { mutableStateOf(false) }

    LaunchedEffect(level.id) {
        tts.speakAndWait(strings.linkInstruction)
        tts.speakSyllable(data.target)
    }

    // Resolución del flash: tras un breve período, se confirma (verde) o se limpia (rojo).
    LaunchedEffect(flashPair) {
        val pair = flashPair ?: return@LaunchedEffect
        delay(900)
        if (flashCorrect) {
            confirmedPair = pair
            delay(250)
            onResult(true)
        } else {
            flashPair = null
            onResult(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .onGloballyPositioned { rootCoords = it }
            .pointerInput(level.id) {
                detectDragGestures(
                    onDragStart = { start ->
                        if (confirmedPair != null || flashPair != null) return@detectDragGestures
                        val hit = leftBoxes.entries
                            .firstOrNull { (_, rect) -> rect.contains(start) }
                            ?.key
                        if (hit != null) {
                            dragSource = hit
                            dragCurrent = start
                        }
                    },
                    onDrag = { change, _ ->
                        if (dragSource != null) {
                            dragCurrent = change.position
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        val src = dragSource
                        val end = dragCurrent
                        dragSource = null
                        dragCurrent = null
                        if (src == null || end == null) return@detectDragGestures
                        val target = rightBoxes.entries
                            .firstOrNull { (_, rect) -> rect.contains(end) }
                            ?.key ?: return@detectDragGestures
                        val isCorrect = src == data.correctLeftIdx && target == data.correctRightIdx
                        flashCorrect = isCorrect
                        flashPair = LinkPair(src, target)
                    },
                    onDragCancel = {
                        dragSource = null
                        dragCurrent = null
                    }
                )
            }
    ) {
        val activeDragLine = dragSource?.let { src ->
            val anchor = leftAnchors[src] ?: return@let null
            val end = dragCurrent ?: return@let null
            anchor to end
        }

        LinkCanvas(
            confirmedPairs = confirmedPair?.let { mapOf(it.left to it.right) } ?: emptyMap(),
            flashPair = flashPair,
            flashCorrect = flashCorrect,
            leftOffsets = leftAnchors,
            rightOffsets = rightAnchors,
            modifier = Modifier.fillMaxSize(),
            dragLine = activeDragLine
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                data.leftItems.forEachIndexed { i, text ->
                    val confirmed = confirmedPair?.left == i
                    val flashing = flashPair?.left == i
                    LinkGameCard(
                        text = text,
                        confirmed = confirmed,
                        flashing = flashing,
                        flashCorrect = flashCorrect,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            rootCoords?.let { root ->
                                val topLeft = root.localPositionOf(coords, Offset.Zero)
                                leftBoxes[i] = Rect(
                                    offset = topLeft,
                                    size = Size(
                                        coords.size.width.toFloat(),
                                        coords.size.height.toFloat()
                                    )
                                )
                                leftAnchors[i] = root.localPositionOf(
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

            Box(modifier = Modifier.weight(0.3f))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                data.rightItems.forEachIndexed { i, text ->
                    val confirmed = confirmedPair?.right == i
                    val flashing = flashPair?.right == i
                    LinkGameCard(
                        text = text,
                        confirmed = confirmed,
                        flashing = flashing,
                        flashCorrect = flashCorrect,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            rootCoords?.let { root ->
                                val topLeft = root.localPositionOf(coords, Offset.Zero)
                                rightBoxes[i] = Rect(
                                    offset = topLeft,
                                    size = Size(
                                        coords.size.width.toFloat(),
                                        coords.size.height.toFloat()
                                    )
                                )
                                rightAnchors[i] = root.localPositionOf(
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

        if (feedback != null) {
            Text(
                text = feedback,
                color = AppColors.FeedbackIncorrect,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun LinkGameCard(
    text: String,
    confirmed: Boolean,
    flashing: Boolean,
    flashCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor: Color = when {
        confirmed -> AppColors.FeedbackCorrect
        flashing && flashCorrect -> AppColors.FeedbackCorrect
        flashing && !flashCorrect -> AppColors.FeedbackIncorrect
        else -> AppColors.BgWhite
    }
    val textColor = if (bgColor == AppColors.BgWhite) AppColors.InkDark else AppColors.BgWhite
    val fontSize = when {
        text.length <= 3 -> 28.sp
        text.length <= 6 -> 20.sp
        text.length <= 9 -> 15.sp
        else -> 12.sp
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 80.dp, minHeight = 56.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private fun buildLinkData(level: Level): LinkGameData {
    val isPhrase = level.word.contains(" ")
    val target = level.syllables.firstOrNull()?.text?.uppercase()
        ?: if (isPhrase) "HOLA" else "MA"
    val pool = (if (isPhrase) ALL_WORDS else ALL_SYLLABLES)
        .filter { !it.equals(target, ignoreCase = true) }
        .shuffled()
    val leftDistractors = pool.take(2)
    val rightDistractors = pool.drop(2).take(2)

    val leftItems = (listOf(target) + leftDistractors).shuffled()
    val rightItems = (listOf(target) + rightDistractors).shuffled()

    return LinkGameData(
        leftItems = leftItems,
        rightItems = rightItems,
        target = target,
        correctLeftIdx = leftItems.indexOfFirst { it.equals(target, ignoreCase = true) },
        correctRightIdx = rightItems.indexOfFirst { it.equals(target, ignoreCase = true) }
    )
}
