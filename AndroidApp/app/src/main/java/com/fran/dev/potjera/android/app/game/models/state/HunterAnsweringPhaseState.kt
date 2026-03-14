package com.fran.dev.potjera.android.app.game.models.state

data class HunterAnsweringPhaseState(
    val hunterWon: Boolean? = null,
    val playersSteps: Int = 0,
    val hunterCorrectAnswers: Int = 0,
    val question: String = "",
    val correctAnswer: String? = null,
    val hunterIsAnswering: Boolean = true,
    val playersAreAnswering: Boolean = false,
    val players: List<PlayersAnsweringPlayer> = emptyList(),
    val hunterAnsweredCorrectly: Boolean? = null, //null -> answer not given yet
    val playersAnsweredCorrectly: Boolean? = null,
    val endTimestamp: Long = 0
)