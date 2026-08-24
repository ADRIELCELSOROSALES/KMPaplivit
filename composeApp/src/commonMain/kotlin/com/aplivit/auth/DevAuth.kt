package com.aplivit.auth

/**
 * Punto ÚNICO de login de desarrollo, hasta implementar el login real de Play Games / Game Center
 * (que requiere el OAuth web client id de Google / la capability de Game Center — config externa).
 *
 * Con [AUTO_LOGIN_TOKEN] != null, la app se auto-loguea con ese JWT si no hay sesión, para poder
 * probar el ciclo completo contra el backend. Poner null (o borrar este archivo y su uso en App.kt)
 * cuando el login real esté listo. NO usar en release.
 */
object DevAuth {
    val AUTO_LOGIN_TOKEN: String? =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5N2U2YTAzYi03ZDQ1LTQ2OTItOTU1My1hMTFiZWIxZDIyZTIiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiOiJTdHVkZW50IiwiZW1haWwiOiIiLCJpc3MiOiJBcGxpdml0IiwiYXVkIjoiQXBsaXZpdCIsImlhdCI6MTc4NzIzMzE2OCwibmJmIjoxNzg3MjMzMTY4LCJleHAiOjE3ODk4MjUxNjh9.hjI0pUlJfDwDerKFmnlUmUyrylChwk33aID963X398s"
}
