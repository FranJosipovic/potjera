package com.fran.dev.potjera.android.app.game.repository

import com.fran.dev.potjera.android.app.game.api.QuizApi
import com.fran.dev.potjera.android.app.game.models.BoardQuestion
import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class QuizRepositoryImpl @Inject constructor(
    private val api: QuizApi
) : QuizRepository {

    override suspend fun getQuickFireQuestions(limit: Int): Result<List<CoinBoosterQuestion>> =
        runCatching { api.getQuickFireQuestions(limit) }

    override suspend fun getMultipleChoiceQuestions(limit: Int): Result<List<BoardQuestion>> =
        runCatching { api.getMultipleChoiceQuestions(limit) }
}
