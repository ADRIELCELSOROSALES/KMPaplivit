package com.aplivit.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animates the tracing of letter strokes on a Canvas.
 *
 * @param strokes      List of [Path] objects in viewport coordinates (0..200 x 0..250).
 * @param playKey      Increment to replay the animation from the beginning.
 * @param viewportWidth  Logical width of the path coordinate space.
 * @param viewportHeight Logical height of the path coordinate space.
 * @param strokeDurationMs Duration in ms for each individual stroke animation.
 * @param onAnimationComplete Called once all strokes finish animating.
 */
@Composable
fun LetterTracingAnimation(
    strokes: List<Path>,
    playKey: Int,
    modifier: Modifier = Modifier,
    viewportWidth: Float = 200f,
    viewportHeight: Float = 250f,
    strokeDurationMs: Int = 800,
    onAnimationComplete: () -> Unit = {}
) {
    val completedStrokes = remember { mutableStateListOf<Path>() }
    val currentProgress = remember { Animatable(0f) }
    var currentStrokeIndex by remember { mutableStateOf(-1) }

    // Reset and re-run the full animation whenever playKey increments
    LaunchedEffect(playKey) {
        if (playKey == 0 || strokes.isEmpty()) return@LaunchedEffect
        completedStrokes.clear()
        currentStrokeIndex = -1

        for (i in strokes.indices) {
            currentStrokeIndex = i
            currentProgress.snapTo(0f)
            currentProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = strokeDurationMs)
            )
            completedStrokes.add(strokes[i])
            if (i < strokes.lastIndex) delay(150)
        }
        currentStrokeIndex = -1
        onAnimationComplete()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (strokes.isEmpty()) return@Canvas

        val scale = minOf(size.width / viewportWidth, size.height / viewportHeight)
        val offsetX = (size.width - viewportWidth * scale) / 2f
        val offsetY = (size.height - viewportHeight * scale) / 2f

        // Stroke width is fixed visually regardless of scale
        val strokeWidthVp = 10.dp.toPx() / scale
        val dotRadiusVp = 12.dp.toPx() / scale

        val guideStyle = Stroke(
            width = strokeWidthVp,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val traceStyle = Stroke(
            width = strokeWidthVp,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        withTransform({
            translate(offsetX, offsetY)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            // 1. Guide paths — faint gray outline of the full letter
            for (stroke in strokes) {
                drawPath(stroke, Color(0xFFDDDDDD), style = guideStyle)
            }

            // 2. Fully completed strokes — drawn in blue
            for (stroke in completedStrokes) {
                drawPath(stroke, Color(0xFF1565C0), style = traceStyle)
            }

            // 3. Currently animating stroke — partial path in blue + leading dot
            val idx = currentStrokeIndex
            if (idx in strokes.indices) {
                val pm = PathMeasure()
                pm.setPath(strokes[idx], false)
                val totalLength = pm.length

                if (totalLength > 0f) {
                    val stopDistance = totalLength * currentProgress.value
                    val partialPath = Path()
                    pm.getSegment(0f, stopDistance, partialPath, startWithMoveTo = true)
                    drawPath(partialPath, Color(0xFF1565C0), style = traceStyle)

                    // Animated leading dot
                    if (currentProgress.value > 0f) {
                        val dotCenter = pm.getPosition(stopDistance)
                        drawCircle(
                            color = Color(0xFFFFC107),
                            radius = dotRadiusVp,
                            center = dotCenter
                        )
                    }
                }
            }
        }
    }
}
