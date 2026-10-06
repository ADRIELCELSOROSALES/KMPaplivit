package com.aplivit.auth

/**
 * Token de desarrollo para probar el ciclo completo contra el backend sin el login nativo
 * (Play Games / Game Center), p. ej. en el simulador de iOS.
 *
 * NUNCA va hardcodeado en el código: cada plataforma lo lee de una fuente local no versionada y
 * SOLO en builds debug. En release devuelve siempre null.
 *  - Android: `aplivit.devJwt=<jwt>` en `local.properties` (o la env var `APLIVIT_DEV_JWT`),
 *    que el build inyecta como `BuildConfig.DEV_AUTH_TOKEN` únicamente en el build type debug.
 *  - iOS: variable de entorno `APLIVIT_DEV_JWT` en el scheme de Xcode (Run > Arguments).
 *
 * El JWT se genera con el DevTokenGenerator del backend.
 */
expect fun devAuthToken(): String?
