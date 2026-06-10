package com.aplivit.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SentenceExercise(
    val id: Int,
    val words: List<String>,         // palabras en orden correcto para formar la oración
    val shuffledIndices: List<Int>   // índices de words en el orden en que se muestran al usuario
)
