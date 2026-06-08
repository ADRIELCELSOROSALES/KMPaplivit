package com.aplivit.presentation.screen.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level
import com.aplivit.presentation.component.AppColors
import kotlinx.coroutines.delay

@Composable
fun DragDropGameScreen(
    level: Level,
    availableSyllables: List<String>,
    arrangedSyllables: List<String>,
    feedback: String?,
    onSyllableMoved: (String) -> Unit,
    onResult: (Boolean) -> Unit
) {
    LaunchedEffect(arrangedSyllables.size) {
        if (arrangedSyllables.isNotEmpty() && arrangedSyllables.size == level.syllables.size) {
            delay(300L)
            val answer = arrangedSyllables.joinToString("")
            onResult(answer == level.word.replace(" ", ""))
        }
    }

    val dropZoneBorderColor = if (feedback != null) AppColors.FeedbackIncorrect else AppColors.Outline

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .border(2.dp, dropZoneBorderColor, RoundedCornerShape(12.dp))
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
                                .background(AppColors.BgWhite, RoundedCornerShape(8.dp))
                                .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                syl,
                                fontSize = dropFontSize,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.InkDark,
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
                        .background(AppColors.BgWhite, RoundedCornerShape(12.dp))
                        .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
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
                        color = AppColors.InkDark,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        if (feedback != null) {
            Text(
                text = feedback,
                color = AppColors.FeedbackIncorrect,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    }
}
