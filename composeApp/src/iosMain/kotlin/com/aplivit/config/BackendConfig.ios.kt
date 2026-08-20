package com.aplivit.config

/**
 * iOS: el simulador comparte la red del host, así que localhost llega directo a la Mac.
 * En dispositivo físico habría que usar la IP LAN de la Mac (misma Wi-Fi).
 */
actual val apiBaseUrl: String = "http://localhost:5050"
