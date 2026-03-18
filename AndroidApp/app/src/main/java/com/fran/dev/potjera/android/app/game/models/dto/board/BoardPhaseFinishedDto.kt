package com.fran.dev.potjera.android.app.game.models.dto.board

import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer

data class BoardPhaseFinishedDto(
    val players: Map<Long, GameSessionPlayer>
)
