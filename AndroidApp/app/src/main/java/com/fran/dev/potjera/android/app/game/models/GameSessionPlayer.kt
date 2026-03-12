package com.fran.dev.potjera.android.app.game.models

data class GameSessionPlayer(
    val playerId: Long,
    val playerName: String,
    val moneyWon: Float,
    val isEliminated: Boolean,
    val isCaptain: Boolean,
    val isHunter: Boolean,
    val isHost: Boolean,
    val hasPlayedBoard: Boolean
)