package com.aplivit.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FixedButton(icon = Icons.Filled.Mic, onClick = onMicClick, iconSize = 48.dp)
            FixedButton(icon = Icons.Filled.Hearing, onClick = onListenClick, iconSize = 48.dp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FixedButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick)
            FixedButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = { if (forwardEnabled) onForwardClick() },
                alpha = if (forwardEnabled) 1f else 0.35f
            )
        }
    }
}

@Composable
private fun FixedButton(
    icon: ImageVector,
    onClick: () -> Unit,
    alpha: Float = 1f,
    iconSize: Dp = 36.dp
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(80.dp)
            .alpha(alpha)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = Color.White
        )
    }
}
