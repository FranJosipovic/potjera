package com.fran.dev.potjera.android.app.game.models.dto.board

data class NewBoardQuestionDto(
    val question: String,
    val choices: List<String>,
    val correctAnswer: String
)
