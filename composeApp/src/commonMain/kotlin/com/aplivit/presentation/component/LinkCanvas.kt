package com.aplivit.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.aplivit.core.domain.model.LinkPair

/**
 * Dibuja las líneas de conexión entre ítems de la columna izquierda y la derecha.
 *
 * [leftOffsets]  — mapa de índice -> punto de anclaje derecho-centro del ítem izquierdo
 * [rightOffsets] — mapa de índice -> punto de anclaje izquierdo-centro del ítem derecho
 * [confirmedPairs] — pares ya validados como correctos (líneas azules permanentes)
 * [flashPair]    — par actualmente en flash (null si no hay flash activo)
 * [flashCorrect] — true = flash azul, false = flash rojo
 */
@Composable
fun LinkCanvas(
    confirmedPairs: Map<Int, Int>,
    flashPair: LinkPair?,
    flashCorrect: Boolean,
    leftOffsets: Map<Int, Offset>,
    rightOffsets: Map<Int, Offset>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()

        // Líneas permanentes de pares correctos confirmados
        confirmedPairs.forEach { (leftIdx, rightIdx) ->
            val start = leftOffsets[leftIdx] ?: return@forEach
            val end = rightOffsets[rightIdx] ?: return@forEach
            drawLine(
                color = AppColors.FeedbackCorrect,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Línea de flash (azul=correcto, rojo=incorrecto)
        flashPair?.let { pair ->
            val start = leftOffsets[pair.left] ?: return@let
            val end = rightOffsets[pair.right] ?: return@let
            drawLine(
                color = if (flashCorrect) AppColors.FeedbackCorrect else AppColors.FeedbackIncorrect,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
