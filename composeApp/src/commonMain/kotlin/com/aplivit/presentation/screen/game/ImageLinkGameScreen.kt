package com.aplivit.presentation.screen.game

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
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
import kmpaplivit.composeapp.generated.resources.Res
import kmpaplivit.composeapp.generated.resources.allDrawableResources
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/**
 * Palabras que cuentan con imagen asociada (archivo en composeResources/drawable nombrado
 * con la palabra en minúscula, p. ej. "cama.jpg"). El juego de unir palabra↔imagen sólo
 * aparece en los niveles cuya palabra esté en esta lista.
 */
internal val IMAGE_WORDS = listOf(
    "CAMA", "TAPA", "TELA", "HOLA", "POLO", "GATO", "BOCA", "DEDO",
    "NIDO", "ROPA", "LUNA", "MANO", "UNO", "LOBO", "LAGO", "NUBE", "ROJO"
)

internal fun levelHasImage(level: Level): Boolean = level.word.uppercase() in IMAGE_WORDS

private data class ImageLinkData(
    val leftWords: List<String>,
    val rightImages: List<String>,
    val target: String,
    val correctLeftIdx: Int,
    val correctRightIdx: Int
)

@Composable
fun ImageLinkGameScreen(
    level: Level,
    feedback: String?,
    strings: AppStrings,
    onResult: (Boolean) -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val data = remember(level.id) { buildImageLinkData(level) }

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
        tts.speakAndWait(strings.imageLinkInstruction)
        tts.speakWord(data.target)
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
            // Columna izquierda: palabras
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                data.leftWords.forEachIndexed { i, word ->
                    WordCard(
                        text = word,
                        confirmed = confirmedPair?.left == i,
                        flashing = flashPair?.left == i,
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

            // Columna derecha: imágenes
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                data.rightImages.forEachIndexed { i, word ->
                    ImageCard(
                        word = word,
                        confirmed = confirmedPair?.right == i,
                        flashing = flashPair?.right == i,
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
private fun WordCard(
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

@Composable
private fun ImageCard(
    word: String,
    confirmed: Boolean,
    flashing: Boolean,
    flashCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor: Color = when {
        confirmed -> AppColors.FeedbackCorrect
        flashing && flashCorrect -> AppColors.FeedbackCorrect
        flashing && !flashCorrect -> AppColors.FeedbackIncorrect
        else -> AppColors.Outline
    }
    val imageRes = Res.allDrawableResources[word.lowercase()]

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 96.dp, minHeight = 80.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(AppColors.BgWhite, RoundedCornerShape(12.dp))
            .border(3.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // Fallback si todavía no se cargó el archivo de imagen para esta palabra.
            Text(
                text = word,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.InkDark,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun buildImageLinkData(level: Level): ImageLinkData {
    val target = level.word.uppercase()
    // Distractores distintos en cada lado: así sólo la palabra objetivo tiene su imagen
    // presente a la derecha → existe exactamente un par válido (como en LinkGameScreen).
    val pool = IMAGE_WORDS.filter { it != target }.shuffled()
    val leftDistractors = pool.take(2)
    val rightDistractors = pool.drop(2).take(2)

    val leftWords = (listOf(target) + leftDistractors).shuffled()
    val rightImages = (listOf(target) + rightDistractors).shuffled()

    return ImageLinkData(
        leftWords = leftWords,
        rightImages = rightImages,
        target = target,
        correctLeftIdx = leftWords.indexOf(target),
        correctRightIdx = rightImages.indexOf(target)
    )
}
