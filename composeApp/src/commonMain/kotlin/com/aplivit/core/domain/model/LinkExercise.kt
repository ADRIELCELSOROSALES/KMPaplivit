package com.aplivit.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LinkExercise(
    val id: Int,
    val type: LinkType,
    val leftItems: List<LinkItem>,
    val rightItems: List<LinkItem>,
    val correctPairs: List<LinkPair>,   // kotlin.Pair no es serializable → se usa LinkPair
    val useSalience: Boolean = true
)

// Equivalente serializable de Pair<Int, Int>
@Serializable
data class LinkPair(val left: Int, val right: Int)

@Serializable
data class LinkItem(
    val text: String? = null,
    val imageRes: String? = null       // nombre del recurso de imagen, null si es texto
)

enum class LinkType {
    SAME_FORMS,          // vincular sílabas iguales
    SAME_WORDS,          // vincular palabras iguales
    WORD_TO_IMAGE,       // vincular palabra con su imagen
    WORDS_TO_SENTENCE    // vincular palabras para formar oración
}
