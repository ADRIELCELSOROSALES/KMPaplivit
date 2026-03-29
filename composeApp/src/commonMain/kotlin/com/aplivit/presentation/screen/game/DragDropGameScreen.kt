package com.aplivit.presentation.screen.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aplivit.core.domain.model.Level

@Composable
fun DragDropGameScreen(level: Level, feedback: String?, onResult: (Boolean) -> Unit) {
    val shuffled = remember { level.syllables.map { it.text }.shuffled().toMutableList() }
    val arranged = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Juego 1: Arrastra las sílabas", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Forma la palabra: ${level.word}", fontSize = 16.sp, color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        // Drop zone
        Text("Tu respuesta:", fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(2.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (arranged.isEmpty()) {
                Text("Suelta las sílabas aquí", color = Color.LightGray)
            } else {
                arranged.forEach { syl ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(syl, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("Sílabas disponibles:", fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            shuffled.toList().forEach { syl ->
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color(0xFF2196F3), RoundedCornerShape(12.dp))
                        .pointerInput(syl) {
                            detectDragGestures(
                                onDragEnd = {
                                    shuffled.remove(syl)
                                    arranged.add(syl)
                                }
                            ) { _, _ -> }
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(syl, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        if (feedback != null) {
            Spacer(Modifier.height(16.dp))
            Text(feedback, color = Color.Red, fontSize = 16.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val answer = arranged.joinToString("")
                onResult(answer == level.word)
            },
            enabled = arranged.size == level.syllables.size
        ) {
            Text("Verificar", fontSize = 18.sp)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                shuffled.addAll(arranged)
                arranged.clear()
            }
        ) {
            Text("Reiniciar", fontSize = 16.sp)
        }
    }
}
