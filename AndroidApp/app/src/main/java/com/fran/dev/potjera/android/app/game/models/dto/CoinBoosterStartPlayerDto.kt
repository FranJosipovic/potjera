package com.fran.dev.potjera.android.app.game.models.dto

import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer

data class CoinBoosterStartPlayerDto(
    val playersInfo: Map<Long, GameSessionPlayer>,
    val questions: List<CoinBoosterQuestion>
)