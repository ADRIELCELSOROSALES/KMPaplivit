package com.aplivit.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Level(
    val id: Int,
    val syllables: List<Syllable>,
    val word: String,
    val instruction: String
)
