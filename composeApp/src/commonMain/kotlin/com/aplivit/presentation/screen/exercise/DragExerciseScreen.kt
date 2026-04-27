package com.aplivit.presentation.screen.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.DragExercise
import com.aplivit.core.domain.model.DragType
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.presentation.component.AppColors
import com.aplivit.presentation.component.BaseExerciseScreen
import org.koin.compose.koinInject
import kotlin.math.roundToInt

// Tamaño mínimo de chip: lo suficiente para sílabas; se expande para palabras largas
private val CHIP_MIN_WIDTH = 64.dp
private val CHIP_HEIGHT = 56.dp
private val CHIP_H_PADDING = 14.dp
private val CHIP_V_PADDING = 10.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DragExerciseScreen(
    exercise: DragExercise,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit
) {
    val tts: SpeechSynthesizer = koinInject()
    val repo: ProgressRepository = koinInject()
    val vm: DragViewModel = remember { DragViewModel(tts, repo) }
    val state by vm.state.collectAsState()

    LaunchedEffect(exercise.id) {
        vm.loadExercise(exercise)
    }

    // ── Drag state (UI layer) ─────────────────────────────────────────────────
    var draggedItemId by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val itemCoords = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    val itemSizes = remember { mutableStateMapOf<Int, IntSize>() }   // px, para centrar el ghost
    val slotRects = remember { mutableStateMapOf<Int, Rect>() }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current

    val instruction = if (exercise.type == DragType.WORDS_TO_SENTENCE)
        "Armá la oración" else "Armá la palabra"

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
                .onGloballyPositioned { rootCoords = it }
        ) {
            // ── Capa de contenido ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = instruction,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.weight(1f))

                // Zona superior: chips arrastrables (desordenados)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.availableItems.forEach { item ->
                        DraggableChip(
                            text = item.text,
                            isBeingDragged = draggedItemId == item.id,
                            onCoordinates = { coords ->
                                itemCoords[item.id] = coords
                                itemSizes[item.id] = coords.size
                            },
                            onDragStart = { localOffset ->
                                draggedItemId = item.id
                                val ic = itemCoords[item.id]
                                dragOffset = if (ic != null && rootCoords != null) {
                                    rootCoords!!.localPositionOf(ic, localOffset)
                                } else localOffset
                            },
                            onDrag = { delta -> dragOffset += delta },
                            onDragEnd = {
                                val targetSlot = slotRects.entries
                                    .firstOrNull { (_, rect) -> rect.contains(dragOffset) }
                                    ?.key
                                if (targetSlot != null && draggedItemId != null) {
                                    vm.onDropOnSlot(draggedItemId!!, targetSlot)
                                }
                                draggedItemId = null
                            }
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                // Zona inferior: slots en orden — FlowRow para que palabras largas se envuelvan
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.slotContents.forEachIndexed { slotIdx, content ->
                        DropSlot(
                            content = content,
                            flash = state.slotFlash[slotIdx],
                            onPositioned = { coords ->
                                rootCoords?.let { root ->
                                    slotRects[slotIdx] = root.localBoundingBoxOf(
                                        sourceCoordinates = coords,
                                        clipBounds = false
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
            }

            // ── Chip fantasma (sigue el dedo durante el drag) ─────────────────
            draggedItemId?.let { id ->
                val text = state.availableItems.firstOrNull { it.id == id }?.text ?: ""
                // Centrar el ghost en el punto de toque usando el tamaño real del chip
                val size = itemSizes[id]
                val halfW = if (size != null) size.width / 2f
                            else with(density) { CHIP_MIN_WIDTH.toPx() / 2f }
                val halfH = if (size != null) size.height / 2f
                            else with(density) { CHIP_HEIGHT.toPx() / 2f }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (dragOffset.x - halfW).roundToInt(),
                                y = (dragOffset.y - halfH).roundToInt()
                            )
                        }
                        .defaultMinSize(minWidth = CHIP_MIN_WIDTH, minHeight = CHIP_HEIGHT)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .background(Color(0xFF1565C0), RoundedCornerShape(12.dp))
                        .padding(horizontal = CHIP_H_PADDING, vertical = CHIP_V_PADDING),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableChip(
    text: String,
    isBeingDragged: Boolean,
    onCoordinates: (LayoutCoordinates) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = CHIP_MIN_WIDTH, minHeight = CHIP_HEIGHT)
            .onGloballyPositioned { onCoordinates(it) }
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(
                color = if (isBeingDragged) Color(0xFF90CAF9) else Color(0xFF2196F3),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(text) {
                detectDragGestures(
                    onDragStart = { offset -> onDragStart(offset) },
                    onDrag = { change, delta ->
                        change.consume()
                        onDrag(delta)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
            .padding(horizontal = CHIP_H_PADDING, vertical = CHIP_V_PADDING),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun DropSlot(
    content: String?,
    flash: FlashState?,
    onPositioned: (LayoutCoordinates) -> Unit
) {
    val bgColor = when {
        content != null -> AppColors.FeedbackCorrect
        flash == FlashState.CORRECT -> AppColors.FeedbackCorrect
        flash == FlashState.INCORRECT -> AppColors.FeedbackIncorrect
        else -> Color.Transparent
    }
    val borderColor = when {
        flash == FlashState.INCORRECT -> AppColors.FeedbackIncorrect
        content != null -> AppColors.FeedbackCorrect
        else -> Color(0xFFBDBDBD)
    }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = CHIP_MIN_WIDTH, minHeight = CHIP_HEIGHT)
            .onGloballyPositioned { onPositioned(it) }
            .shadow(if (content != null) 4.dp else 0.dp, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = CHIP_H_PADDING, vertical = CHIP_V_PADDING),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            Text(
                text = content,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
