package com.aplivit.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VocalizeExercise(
    val id: Int,
    val type: VocalizeType,
    val content: String   // sílaba, palabra u oración a vocalizar
)

enum class VocalizeType {
    SYLLABLE,
    WORD,
    SENTENCE
}
