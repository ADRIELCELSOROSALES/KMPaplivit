package com.aplivit.config

/**
 * Base URL del backend aplivlit, resuelta por plataforma.
 * Ver BackendConfig.android.kt / BackendConfig.ios.kt.
 *
 * La autenticación ya NO usa un token hardcodeado: el JWT del alumno lo maneja
 * [com.aplivit.auth.TokenStore] / [com.aplivit.auth.SessionManager]. Para probar en dev sin el
 * login nativo, usar `SessionManager.useTokenForDev(jwt)` con un token generado por el
 * DevTokenGenerator del backend.
 */
expect val apiBaseUrl: String
