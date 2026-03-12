package com.fran.dev.potjera.android.app.game.models.dto

import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer

data class CoinBoosterStartHunterDto(
    val playersInfo: Map<Long, GameSessionPlayer>
)