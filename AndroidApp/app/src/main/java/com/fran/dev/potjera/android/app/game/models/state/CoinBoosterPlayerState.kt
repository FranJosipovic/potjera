package com.fran.dev.potjera.android.app.game.models.state

import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion

data class CoinBoosterPlayerState(
    val playerId: Long = 0,
    val correctAnswers: Int = 0,
    val questions:List<CoinBoosterQuestion> = emptyList(),
    val isFinished: Boolean = false
)
