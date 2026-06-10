package com.aplivit.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioPairExercise(
    val id: Int,
    val target: String,
    val options: List<String>,
    val useSalience: Boolean = true
) {
    init {
        require(options.size == 4) { "AudioPairExercise requiere exactamente 4 opciones" }
        val correctCount = options.count { it.equals(target, ignoreCase = true) }
        require(correctCount == 2) {
            "AudioPairExercise requiere exactamente 2 opciones que coincidan con target (case-insensitive), encontradas: $correctCount"
        }
    }
}
