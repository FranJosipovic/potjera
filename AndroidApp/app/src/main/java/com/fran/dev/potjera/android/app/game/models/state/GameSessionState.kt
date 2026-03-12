package com.fran.dev.potjera.android.app.game.models.state

import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.game.models.enums.GamePhase

data class GameSessionState(
    val gameSessionId: String = "",
    val gamePhase: GamePhase = GamePhase.STARTING,
    val gameSessionPlayers: Map<Long, GameSessionPlayer> = emptyMap(),
)