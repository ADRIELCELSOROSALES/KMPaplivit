package com.aplivit.presentation.screen.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level
import com.aplivit.shared.AppStrings

@Composable
fun DragDropGameScreen(
    level: Level,
    availableSyllables: List<String>,
    arrangedSyllables: List<String>,
    feedback: String?,
    strings: AppStrings,
    onSyllableMoved: (String) -> Unit,
    onReset: () -> Unit,
    onResult: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Drop zone
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .border(2.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (arrangedSyllables.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    arrangedSyllables.forEach { syl ->
                        val dropFontSize = when {
                            syl.length <= 3 -> 22.sp
                            syl.length <= 6 -> 16.sp
                            else            -> 13.sp
                        }
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 60.dp, minHeight = 52.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                syl,
                                fontSize = dropFontSize,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableSyllables.forEach { syl ->
                val availFontSize = when {
                    syl.length <= 3 -> 24.sp
                    syl.length <= 6 -> 17.sp
                    else            -> 13.sp
                }
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 64.dp, minHeight = 52.dp)
                        .background(Color(0xFF2196F3), RoundedCornerShape(12.dp))
                        .pointerInput(syl) {
                            detectDragGestures(
                                onDragEnd = { onSyllableMoved(syl) }
                            ) { _, _ -> }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        syl,
                        fontSize = availFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        if (feedback != null) {
            Text(
                text = feedback,
                color = Color.Red,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val answer = arrangedSyllables.joinToString("")
                    onResult(answer == level.word.replace(" ", ""))
                },
                enabled = arrangedSyllables.size == level.syllables.size
            ) {
                Text(strings.verify, fontSize = 18.sp)
            }
            Button(onClick = onReset) {
                Text(strings.reset, fontSize = 16.sp)
            }
        }
    }
}
