package com.aplivit.infrastructure.remote

import com.aplivit.config.apiBaseUrl
import com.aplivit.infrastructure.remote.dto.RemoteLanguage
import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ChangeLanguageRequest(@SerialName("language") val language: RemoteLanguage)

/** Idioma de preferencia del alumno (RF-09). El backend sirve el contenido en este idioma. */
class MyLanguageApi(private val client: HttpClient) {

    suspend fun setLanguage(language: RemoteLanguage) {
        client.put("$apiBaseUrl/api/my-language") {
            setBody(ChangeLanguageRequest(language))
        }
    }
}
