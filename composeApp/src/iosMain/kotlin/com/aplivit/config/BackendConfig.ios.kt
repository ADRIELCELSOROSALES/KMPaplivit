package com.aplivit.config

/**
 * iOS: en un iPhone FÍSICO, localhost es el propio teléfono (no hay adb reverse como en Android),
 * y la Wi-Fi corporativa (10.10.4.x) tiene client isolation: la IP LAN de la Mac NO le llega al
 * teléfono. La vía que funciona es un túnel HTTPS público de Cloudflare contra el backend local:
 *
 *   cloudflared tunnel --url http://localhost:5050
 *
 * y pegar acá la URL trycloudflare.com que imprime (cambia en cada arranque del túnel).
 *
 * Simulador: puede usar "http://localhost:5050" directo, sin túnel.
 */
actual val apiBaseUrl: String = "https://encoding-barn-matrix-springer.trycloudflare.com"
