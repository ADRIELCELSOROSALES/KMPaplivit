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
import androidx.compose.ui.unit.dp

@Composable
fun BaseExerciseScreen(
    onMicClick: () -> Unit,
    onListenClick: () -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    forwardEnabled: Boolean,
    content: @Composable () -> Unit
) {
    Surface(color = AppColors.BgWhite, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopIconButton(Icons.Filled.Mic, onMicClick)
                TopIconButton(Icons.Filled.Hearing, onListenClick)
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NavButton(Icons.AutoMirrored.Filled.ArrowBack, onBackClick, Modifier.weight(1f))
                NavButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = { if (forwardEnabled) onForwardClick() },
                    modifier = Modifier.weight(1f),
                    alpha = if (forwardEnabled) 1f else 0.35f,
                )
            }
        }
    }
}

@Composable
private fun TopIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .background(AppColors.BgWhite, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.Outline, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
            Icon(icon, contentDescription = null, tint = AppColors.InkDark, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    Box(
        modifier
            .height(72.dp)
            .background(AppColors.BgWhite, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.OutlineSoft, RoundedCornerShape(18.dp))
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null, tint = AppColors.InkDark.copy(alpha = 0.55f), modifier = Modifier.size(32.dp))
        }
    }
}
