package com.aplivit.offline

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.infrastructure.remote.dto.RemoteLanguage

/** Mapea el idioma de la app al enum del backend (RF-09). */
fun AppLanguage.toRemote(): RemoteLanguage = when (this) {
    AppLanguage.SPANISH -> RemoteLanguage.Spanish
    AppLanguage.ENGLISH -> RemoteLanguage.English
    AppLanguage.FRENCH -> RemoteLanguage.French
}
