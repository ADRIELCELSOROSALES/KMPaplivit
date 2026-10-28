package com.aplivit.config

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/**
 * Backend de desarrollo en iOS: en un iPhone FÍSICO, localhost es el propio teléfono (no hay
 * adb reverse como en Android), y la Wi-Fi corporativa (10.10.4.x) tiene client isolation, así
 * que la IP LAN de la Mac tampoco le llega. La vía que funciona es un túnel HTTPS público de
 * Cloudflare contra el backend local:
 *
 *   cloudflared tunnel --url http://localhost:5050
 *
 * y pegar acá la URL trycloudflare.com que imprime (cambia en cada arranque del túnel).
 * El simulador puede usar "http://localhost:5050" directo, sin túnel.
 */
private const val DEV_API_BASE_URL = "https://exhibits-from-xhtml-touched.trycloudflare.com"

@OptIn(ExperimentalNativeApi::class)
actual val apiBaseUrl: String =
    if (Platform.isDebugBinary) DEV_API_BASE_URL else PRODUCTION_API_BASE_URL
