package com.aplivit.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Lectura de los claims de un JWT SIN validar la firma. Alcanza para lo que el cliente necesita
 * saber del token que ya le dio el backend: cuándo vence (`exp`) y de qué alumno es (`sub`).
 *
 * La validación real es del backend; acá solo se evita mandar un Bearer que ya sabemos vencido y
 * detectar un cambio de cuenta. No decide permisos.
 */
object Jwt {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Vencimiento en epoch-segundos, o null si el token no trae `exp` (o no es un JWT). */
    fun expEpochSeconds(token: String): Long? = claim(token, "exp")?.longOrNull

    /** Id del alumno (claim `sub`), o null si no está. */
    fun subject(token: String): String? = claim(token, "sub")?.content

    private fun claim(token: String, name: String): JsonPrimitive? = runCatching {
        val parts = token.split('.')
        if (parts.size < 2) return null
        val payload = base64UrlDecode(parts[1]) ?: return null
        val claims = json.parseToJsonElement(payload) as? JsonObject ?: return null
        claims[name] as? JsonPrimitive
    }.getOrNull()

    /**
     * Base64url a mano: `kotlin.io.encoding.Base64` sigue siendo experimental y exige padding
     * exacto, que los JWT no traen. Acepta también el alfabeto estándar (+, /) por robustez.
     */
    private fun base64UrlDecode(value: String): String? {
        var buffer = 0
        var bits = 0
        val bytes = ArrayList<Byte>(value.length * 3 / 4 + 3)
        for (char in value) {
            if (char == '=') break
            val index = ALPHABET.indexOf(
                when (char) {
                    '+' -> '-'
                    '/' -> '_'
                    else -> char
                }
            )
            if (index < 0) return null
            buffer = (buffer shl 6) or index
            bits += 6
            if (bits >= 8) {
                bits -= 8
                bytes.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    private const val ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
}
