package com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase

data class PlayersAnsweringStartDto(
    val playersAnsweringState: PlayersAnsweringStateDto,
    val question: PlayersAnsweringQuestionDto,
    val questionNum: Int
)