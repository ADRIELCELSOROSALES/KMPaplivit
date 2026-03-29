package com.aplivit.core.domain.usecase

import com.aplivit.core.domain.model.Level
import com.aplivit.infrastructure.content.LevelsLoader

class GetLevelsUseCase(private val loader: LevelsLoader) {
    suspend operator fun invoke(): List<Level> = loader.load()
}
