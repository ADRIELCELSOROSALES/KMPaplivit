package com.aplivit.offline

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.infrastructure.remote.dto.RemoteLanguage

/** Mapea el idioma de la app al enum del backend (RF-09). */
fun AppLanguage.toRemote(): RemoteLanguage = when (this) {
    AppLanguage.SPANISH -> RemoteLanguage.Spanish
    AppLanguage.ENGLISH -> RemoteLanguage.English
    AppLanguage.FRENCH -> RemoteLanguage.French
}

/**
 * Código de idioma de la app ("es"/"en"/"fr") para el idioma que devolvió el backend, o null si
 * ese idioma no existe en la app (ej. HaitianCreole, que el backend soporta y la app todavía no).
 */
fun RemoteLanguage.toAppCodeOrNull(): String? = when (this) {
    RemoteLanguage.Spanish -> AppLanguage.SPANISH.code
    RemoteLanguage.English -> AppLanguage.ENGLISH.code
    RemoteLanguage.French -> AppLanguage.FRENCH.code
    RemoteLanguage.HaitianCreole -> null
}
