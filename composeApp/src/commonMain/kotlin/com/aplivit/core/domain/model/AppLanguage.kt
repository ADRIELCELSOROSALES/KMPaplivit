package com.aplivit.core.domain.model

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val ttsLocale: String
) {
    SPANISH("es", "Español", "es-ES"),
    ENGLISH("en", "English", "en-US"),
    FRENCH("fr", "Français", "fr-FR")
}
