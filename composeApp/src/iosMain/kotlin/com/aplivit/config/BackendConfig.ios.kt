package com.aplivit.config

/**
 * iOS: en un iPhone FÍSICO, localhost es el propio teléfono (no hay adb reverse como en Android),
 * así que hay que apuntar a la IP LAN de la Mac (misma Wi-Fi). Esta IP también funciona desde el
 * simulador. Actualizar si cambia la red/IP de la Mac (ver `ipconfig getifaddr en0`).
 *
 * Simulador solo: podría usarse "http://localhost:5050".
 */
actual val apiBaseUrl: String = "http://10.10.4.103:5050"
