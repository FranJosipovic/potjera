package com.fran.dev.potjera.android.app.game.models.dto.hunterphase

data class HunterAnsweringStateDto(
    val hunterCorrectAnswers: Int = 0,
    val totalStepsToReach: Int = 0,
    val currentQuestionIndex: Int = 0,
    val hunterJustWrong: Boolean = false
)