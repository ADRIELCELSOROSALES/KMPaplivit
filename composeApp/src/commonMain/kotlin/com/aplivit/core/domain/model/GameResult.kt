package com.aplivit.core.domain.model

sealed class GameResult {
    object Success : GameResult()
    data class Failure(val attempts: Int) : GameResult()
}
