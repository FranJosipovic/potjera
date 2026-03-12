package com.fran.dev.potjera.android.app.game.models.dto.board

import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer

data class PlayerWonDto(
    val playerWonId: Long,
    val moneyWon: Float,
    val playersListUpdated:  Map<Long, GameSessionPlayer>
)
