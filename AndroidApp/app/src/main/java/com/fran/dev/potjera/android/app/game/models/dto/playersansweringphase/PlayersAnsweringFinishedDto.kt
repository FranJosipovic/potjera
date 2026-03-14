package com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase

data class PlayersAnsweringFinishedDto(
    val correctAnswers: Int,
    val playerIds: List<Long>
)