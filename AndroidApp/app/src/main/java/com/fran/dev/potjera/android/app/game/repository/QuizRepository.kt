package com.fran.dev.potjera.android.app.game.repository

import com.fran.dev.potjera.android.app.game.models.BoardQuestion
import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion

interface QuizRepository {
    suspend fun getQuickFireQuestions(limit: Int = 10): Result<List<CoinBoosterQuestion>>
    suspend fun getMultipleChoiceQuestions(limit: Int = 10): Result<List<BoardQuestion>>
}