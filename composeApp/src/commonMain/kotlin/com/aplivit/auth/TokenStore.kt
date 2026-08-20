package com.aplivit.auth

import com.russhwolf.settings.Settings

/**
 * Guarda el JWT del alumno de forma persistente (sobrevive reinicios) para poder jugar y
 * sincronizar sin volver a loguearse en cada apertura. Fuente única del Bearer que usa el
 * HttpClient en cada request.
 */
class TokenStore(private val settings: Settings) {

    fun save(token: String, expiresAtUtc: String?) {
        settings.putString(KEY_TOKEN, token)
        if (expiresAtUtc != null) settings.putString(KEY_EXPIRES, expiresAtUtc)
    }

    /** JWT vigente, o null si no hay sesión. */
    fun token(): String? = settings.getStringOrNull(KEY_TOKEN)

    fun expiresAtUtc(): String? = settings.getStringOrNull(KEY_EXPIRES)

    fun isSignedIn(): Boolean = token() != null

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_EXPIRES)
    }

    private companion object {
        const val KEY_TOKEN = "auth.jwt.v1"
        const val KEY_EXPIRES = "auth.jwt.expires.v1"
    }
}
