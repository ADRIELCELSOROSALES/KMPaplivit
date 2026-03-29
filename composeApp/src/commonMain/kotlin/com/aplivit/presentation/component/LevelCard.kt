package com.aplivit.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level

@Composable
fun LevelCard(
    level: Level,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isCompleted -> Color(0xFF4CAF50)
        isUnlocked -> Color(0xFF2196F3)
        else -> Color(0xFF9E9E9E)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(if (isUnlocked) 4.dp else 0.dp, RoundedCornerShape(16.dp))
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .then(if (isUnlocked) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (isUnlocked) 1f else 0.5f)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Nivel ${level.id}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = level.word,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = when {
                    isCompleted -> "Completado"
                    isUnlocked -> "Disponible"
                    else -> "Bloqueado"
                },
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        if (!isUnlocked) {
            Text(
                text = "🔒",
                fontSize = 24.sp,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}
