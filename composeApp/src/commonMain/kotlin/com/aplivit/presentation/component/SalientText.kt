package com.aplivit.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Composable que envuelve contenido tocable y le aplica un efecto de salience al ser tapeado:
 * escala 1.0 → 1.2 → 1.0 en 200 ms (100 ms subida + 100 ms bajada).
 *
 * El efecto escala TODO el contenido envuelto (texto, fondo, sombra incluidos) ya que
 * [graphicsLayer] precede a cualquier decoración visual dentro del slot [content].
 *
 * @param onClick     Acción al tocar.
 * @param modifier    Modificador externo (posición, tamaño, etc.).
 * @param salientEnabled  Si es false, la animación se omite y solo ejecuta el click.
 * @param content     Contenido a escalar.
 */
@Composable
fun SalientText(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    salientEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable {
                onClick()
                if (salientEnabled) {
                    scope.launch {
                        scale.animateTo(
                            targetValue = 1.2f,
                            animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
                        )
                        scale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
                        )
                    }
                }
            }
    ) {
        content()
    }
}
