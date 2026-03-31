package com.aplivit.infrastructure.content

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.Level
import com.aplivit.core.domain.model.Syllable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kmpaplivit.composeapp.generated.resources.Res

@Serializable
private data class LevelDto(
    val id: Int,
    val syllables: List<String>,
    val word: String,
    val instruction: String
)

private fun LevelDto.toDomain() = Level(
    id = id,
    syllables = syllables.map { Syllable(it) },
    word = word,
    instruction = instruction
)

class LevelsLoader {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(language: AppLanguage = AppLanguage.SPANISH): List<Level> {
        val bytes = Res.readBytes("files/levels_${language.code}.json")
        val dtos: List<LevelDto> = json.decodeFromString(bytes.decodeToString())
        return dtos.map { it.toDomain() }
    }
}
