package com.aplivit.core.domain.usecase

import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.domain.model.Level
import com.aplivit.infrastructure.content.LevelsLoader

class GetLevelsUseCase(private val levelsLoader: LevelsLoader) {
    suspend fun execute(language: AppLanguage = AppLanguage.SPANISH): List<Level> =
        levelsLoader.load(language)
}
