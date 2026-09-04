package com.aplivit.auth

import com.aplivit.infrastructure.nowEpochSeconds
import com.russhwolf.settings.Settings

/**
 * Guarda el JWT del alumno de forma persistente (sobrevive reinicios) para poder jugar y
 * sincronizar sin volver a loguearse en cada apertura. Fuente única del Bearer que usa el
 * HttpClient en cada request.
 *
 * El token se considera vigente solo hasta su `exp` (menos un margen de reloj): un JWT vencido
 * NO se manda —el backend lo rechazaría con 401— y la app lo trata como "sin sesión", lo que
 * dispara el re-login en el arranque y en el interceptor de 401 del [com.aplivit.infrastructure.remote.createApiHttpClient].
 */
class TokenStore(private val settings: Settings) {

    fun save(token: String, expiresAtUtc: String?) {
        settings.putString(KEY_TOKEN, token)

        if (expiresAtUtc != null) settings.putString(KEY_EXPIRES, expiresAtUtc)
        else settings.remove(KEY_EXPIRES)

        // El vencimiento se toma del propio JWT (`exp`, epoch-segundos): está siempre presente y
        // no depende de que el endpoint de login mande `expires_at_utc` (el token de dev no lo trae).
        val exp = Jwt.expEpochSeconds(token)
        if (exp != null) settings.putLong(KEY_EXPIRES_EPOCH, exp) else settings.remove(KEY_EXPIRES_EPOCH)

        val studentId = Jwt.subject(token)
        if (studentId != null) settings.putString(KEY_STUDENT_ID, studentId)
        else settings.remove(KEY_STUDENT_ID)
    }

    /** JWT vigente, o null si no hay sesión o si ya venció. */
    fun token(): String? = if (isExpired()) null else settings.getStringOrNull(KEY_TOKEN)

    fun expiresAtUtc(): String? = settings.getStringOrNull(KEY_EXPIRES)

    /**
     * Id del alumno dueño del token guardado (claim `sub`), incluso si el token está vencido:
     * sirve para detectar un cambio de cuenta antes de renovar la sesión.
     */
    fun studentId(): String? = settings.getStringOrNull(KEY_STUDENT_ID)

    fun isSignedIn(): Boolean = token() != null

    /** true si hay un token guardado y su `exp` ya pasó (con margen de reloj). */
    fun isExpired(): Boolean {
        if (settings.getStringOrNull(KEY_TOKEN) == null) return false
        // Sin `exp` conocido no se puede afirmar que venció: se deja que el backend decida (401).
        val exp = settings.getLongOrNull(KEY_EXPIRES_EPOCH) ?: return false
        return nowEpochSeconds() >= exp - CLOCK_SKEW_SECONDS
    }

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_EXPIRES)
        settings.remove(KEY_EXPIRES_EPOCH)
        // KEY_STUDENT_ID NO se borra acá: el AccountGuard lo compara contra el próximo login para
        // saber si el espejo local (progreso/cache) es de otra cuenta.
    }

    private companion object {
        const val KEY_TOKEN = "auth.jwt.v1"
        const val KEY_EXPIRES = "auth.jwt.expires.v1"
        const val KEY_EXPIRES_EPOCH = "auth.jwt.expires.epoch.v1"
        const val KEY_STUDENT_ID = "auth.jwt.student.v1"

        /** Margen para relojes desfasados: un token que vence en <60s ya se trata como vencido. */
        const val CLOCK_SKEW_SECONDS = 60L
    }
}
