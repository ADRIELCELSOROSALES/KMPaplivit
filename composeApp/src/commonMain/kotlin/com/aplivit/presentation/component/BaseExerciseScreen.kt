package com.aplivit.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aplivit.presentation.util.rememberIsLandscape

@Composable
fun BaseExerciseScreen(
    onMicClick: () -> Unit,
    onListenClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    forwardEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val isLandscape = rememberIsLandscape()

    // En landscape: misma estructura (arriba / centro / abajo) pero más compacta.
    val topPadH = if (isLandscape) 16.dp else 20.dp
    val topPadV = if (isLandscape) 8.dp else 16.dp
    val botPadH = if (isLandscape) 16.dp else 24.dp
    val botPadV = if (isLandscape) 8.dp else 20.dp
    val iconBtnSize = if (isLandscape) 52.dp else 72.dp
    val iconSize = if (isLandscape) 26.dp else 36.dp
    val navBtnHeight = 72.dp
    val navIconSize = 46.dp
    val cornerRadius = if (isLandscape) 14.dp else 18.dp

    Surface(color = AppColors.BgWhite, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            // Fila superior: Mic (izquierda) | Hearing (derecha)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = topPadH, vertical = topPadV),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopIconButton(Icons.Filled.Mic, onMicClick, iconBtnSize, iconSize, cornerRadius)
                TopIconButton(Icons.Filled.Hearing, onListenClick, iconBtnSize, iconSize, cornerRadius)
            }

            // Centro: contenido de la actividad
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }

            // Fila inferior: Atrás (izquierda) | Adelante (derecha)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = botPadH, vertical = botPadV),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NavButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f),
                    height = navBtnHeight,
                    iconSize = navIconSize,
                    cornerRadius = cornerRadius,
                )
                NavButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = { if (forwardEnabled) onForwardClick() },
                    modifier = Modifier.weight(1f),
                    height = navBtnHeight,
                    iconSize = navIconSize,
                    cornerRadius = cornerRadius,
                    alpha = if (forwardEnabled) 1f else 0.35f,
                )
            }
        }
    }
}

@Composable
private fun TopIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    btnSize: Dp,
    iconSize: Dp,
    cornerRadius: Dp,
) {
    Box(
        Modifier
            .size(btnSize)
            .background(AppColors.BgWhite, RoundedCornerShape(cornerRadius))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(btnSize)) {
            Icon(icon, contentDescription = null, tint = AppColors.InkDark, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    iconSize: Dp = 32.dp,
    cornerRadius: Dp = 18.dp,
    alpha: Float = 1f,
) {
    Box(
        modifier
            .height(height)
            .background(AppColors.BgWhite, RoundedCornerShape(cornerRadius))
            .border(1.dp, AppColors.OutlineSoft, RoundedCornerShape(cornerRadius))
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null, tint = AppColors.InkDark, modifier = Modifier.size(iconSize))
        }
    }
}
