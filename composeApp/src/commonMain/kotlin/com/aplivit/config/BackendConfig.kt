package com.aplivit.config

/**
 * URL del backend aplivlit.
 *
 * Se resuelve por plataforma y por tipo de build: en debug apunta al backend de desarrollo de
 * cada quien (ver BackendConfig.android.kt / BackendConfig.ios.kt), y en release SIEMPRE a
 * [PRODUCTION_API_BASE_URL]. Asi una build de release no puede salir apuntando sin querer a la
 * maquina de un desarrollador.
 *
 * La autenticación no usa un token hardcodeado: el JWT del alumno lo maneja
 * [com.aplivit.auth.TokenStore] / [com.aplivit.auth.SessionManager]. Para probar en dev sin el
 * login nativo, ver [com.aplivit.auth.devAuthToken].
 */
expect val apiBaseUrl: String

/**
 * Backend de producción, el mismo para iOS y Android.
 *
 * TODO(deploy): reemplazar cuando el backend esté desplegado. Con este valor, una build de
 * release no puede hablar con ningún backend: el host no existe.
 */
const val PRODUCTION_API_BASE_URL = "https://REEMPLAZAR-URL-DEL-BACKEND"
