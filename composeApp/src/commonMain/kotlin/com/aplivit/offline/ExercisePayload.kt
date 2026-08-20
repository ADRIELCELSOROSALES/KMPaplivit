package com.aplivit.offline

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Schema del `payload` opaco del ejercicio. El backend NO lo interpreta: lo define y lo posee la
 * app (mismo shape que emite tools/content-export). Todos los campos son opcionales para que el
 * parseo sea resiliente ante contenido viejo o de otro tipo.
 *
 * `appType` es el discriminador real del tipo de actividad de la app (más fino que el ExerciseType
 * del backend): VOCALIZE, TOUCH, LINK, SENTENCE, AUDIO_PAIR.
 */
@Serializable
data class ExercisePayload(
    @SerialName("appType") val appType: String? = null,
    // VOCALIZE
    @SerialName("vocalizeType") val vocalizeType: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("instruction") val instruction: String? = null,
    @SerialName("language") val language: String? = null
    // (TOUCH/LINK/SENTENCE/AUDIO_PAIR agregan sus campos ricos acá cuando se autoren.)
)
