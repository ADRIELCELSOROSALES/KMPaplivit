package com.aplivit.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            FixedButton(icon = "🎤", onClick = onMicClick)
            FixedButton(icon = "👂", onClick = onListenClick)
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
            FixedButton(icon = "◀", onClick = onBackClick)
            FixedButton(
                icon = "▶",
                onClick = { if (forwardEnabled) onForwardClick() },
                alpha = if (forwardEnabled) 1f else 0.35f
            )
        }
    }
}

@Composable
private fun FixedButton(
    icon: String,
    onClick: () -> Unit,
    alpha: Float = 1f
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
        modifier = Modifier
            .size(80.dp)
            .alpha(alpha)
    ) {
        Text(text = icon, fontSize = 28.sp)
    }
}
