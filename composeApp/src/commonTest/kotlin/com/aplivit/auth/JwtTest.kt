package com.aplivit.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtTest {

    // Header/payload base64url sin padding, igual que emite el backend. La firma es irrelevante:
    // el cliente no la valida, solo lee claims para saber si el token venció y de quién es.
    // payload: {"sub":"0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0","exp":1789825168,"iss":"Aplivit"}
    private val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJzdWIiOiIwZjFlMmQzYy00YjVhLTY5NzgtODc5Ni1hNWI0YzNkMmUxZjAiLCJleHAiOjE3ODk4MjUxNjgsImlzcyI6IkFwbGl2aXQifQ." +
        "firma-que-no-se-valida"

    @Test
    fun lee_el_vencimiento_y_el_alumno() {
        assertEquals(1789825168L, Jwt.expEpochSeconds(token))
        assertEquals("0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0", Jwt.subject(token))
    }

    @Test
    fun tolera_un_token_que_no_es_jwt() {
        assertNull(Jwt.expEpochSeconds("no-es-un-jwt"))
        assertNull(Jwt.subject(""))
        assertNull(Jwt.subject("a.b.c"))
    }
}
