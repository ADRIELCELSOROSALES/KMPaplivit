package com.aplivit.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DragExercise(
    val id: Int,
    val type: DragType,
    val items: List<String>,   // elementos en orden correcto (sílabas o palabras)
    val targetSlots: Int       // cantidad de slots; normalmente == items.size
)

enum class DragType {
    SYLLABLES_TO_WORD,
    WORDS_TO_SENTENCE
}
