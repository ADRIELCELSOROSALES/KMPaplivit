package com.aplivit.config

/**
 * Modo 100% offline: la app no hace login ni le pega al backend; juega solo con el contenido
 * local empaquetado (levels_<code>.json) y guarda el progreso en el dispositivo.
 *
 * Pensado para builds de prueba que se instalan sin backend disponible. Se activa por build:
 *  - iOS: `APLIVIT_OFFLINE_ONLY=YES` como build setting de Xcode (ver Config.xcconfig), que
 *    llega al Info.plist como `AplivitOfflineOnly`.
 *  - Android: siempre apagado por ahora.
 */
expect val offlineOnly: Boolean
