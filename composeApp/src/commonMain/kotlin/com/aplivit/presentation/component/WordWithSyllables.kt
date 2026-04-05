package com.aplivit.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WordWithSyllables(
    word: String,
    syllables: List<String>,
    onSyllableTap: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    syllableColors: Map<Int, Color> = emptyMap(),
    enabledIndices: Set<Int> = syllables.indices.toSet(),
    salientEnabled: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (word.isNotEmpty()) {
            Text(
                text = word,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            syllables.forEachIndexed { index, syllable ->
                val bgColor = syllableColors[index] ?: Color(0xFF4CAF50)
                val enabled = index in enabledIndices

                SalientText(
                    onClick = { onSyllableTap(index) },
                    salientEnabled = salientEnabled && enabled
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .background(bgColor, RoundedCornerShape(12.dp))
                            // El clickable está en SalientText; aquí solo color y forma
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = syllable,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
