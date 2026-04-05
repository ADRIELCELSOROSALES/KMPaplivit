package com.aplivit.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TouchExercise(
    val id: Int,
    val type: TouchType,
    val target: String,
    val options: List<String>,
    val correctIndices: List<Int>,
    val useSalience: Boolean = true
)

enum class TouchType {
    SIMILAR_FORMS,
    SPECIFIC_FORM,
    SYLLABLE_IN_WORD,
    ORDER_SYLLABLES,
    ORDER_WORDS
}
