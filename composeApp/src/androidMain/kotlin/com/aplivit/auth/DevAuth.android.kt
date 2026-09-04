package com.aplivit.auth

import com.aplivit.BuildConfig

/**
 * El token llega por `BuildConfig.DEV_AUTH_TOKEN`, que el build type release fija en "" (nunca
 * queda en el APK de release) y debug toma de `local.properties` / `APLIVIT_DEV_JWT`. El chequeo
 * de `DEBUG` es la segunda barrera, por si alguien cambia esa inyección.
 */
actual fun devAuthToken(): String? {
    if (!BuildConfig.DEBUG) return null
    return BuildConfig.DEV_AUTH_TOKEN.takeIf { it.isNotBlank() }
}
