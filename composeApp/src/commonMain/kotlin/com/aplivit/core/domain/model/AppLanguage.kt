package com.aplivit.core.domain.model

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val ttsLocale: String
) {
    SPANISH("es", "Español", "es-ES"),
    ENGLISH("en", "English", "en-US"),
    FRENCH("fr", "Français", "fr-FR");

    companion object {
        /** 1 = Español, 2 = Francés, 3 = Inglés */
        fun fromCode(code: Int): AppLanguage = when (code) {
            1 -> SPANISH
            2 -> FRENCH
            3 -> ENGLISH
            else -> SPANISH
        }
    }
}
