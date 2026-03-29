package com.aplivit.infrastructure.content

import kmpaplivit.composeapp.generated.resources.Res
import com.aplivit.core.domain.model.Level
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

class LevelsLoader {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): List<Level> {
        val bytes = Res.readBytes("files/levels.json")
        return json.decodeFromString(bytes.decodeToString())
    }
}
