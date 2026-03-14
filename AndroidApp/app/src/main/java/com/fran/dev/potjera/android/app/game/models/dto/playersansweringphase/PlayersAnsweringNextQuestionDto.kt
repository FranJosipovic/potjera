package com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase

data class PlayersAnsweringNextQuestionDto(
    val question: PlayersAnsweringQuestionDto,
    val questionNum: Int,
    val total: Int
)
