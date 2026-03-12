package com.fran.dev.potjera.android.app.game.models.dto.board

import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer

data class PlayerCaughtDto(
    val playerCaughtId: Long,
    val playersListUpdated: Map<Long, GameSessionPlayer>
)
