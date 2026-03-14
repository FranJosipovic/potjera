package com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase

data class PlayersAnsweringStateDto(
    val correctAnswers: Int = 0,
    val signedPlayerId: Long? = null,
    val currentQuestionIndex: Int = 0
)