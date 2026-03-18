package com.fran.dev.potjera.android.app.game.api

import com.fran.dev.potjera.android.app.game.models.BoardQuestion
import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion
import retrofit2.http.GET
import retrofit2.http.Query

interface QuizApi {
    @GET("quiz/quick-fire")
    suspend fun getQuickFireQuestions(
        @Query("limit") limit: Int = 10
    ): List<CoinBoosterQuestion>

    @GET("quiz/multiple-choice")
    suspend fun getMultipleChoiceQuestions(
        @Query("limit") limit: Int = 10
    ): List<BoardQuestion>
}