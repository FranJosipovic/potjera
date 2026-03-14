package com.fran.dev.potjera.android.app.game.models.dto.board

data class BoardPhaseStartingDto(
    val currentPlayerId: Long,
    val boardState: PlayerVHunterBoardStateDto
)
