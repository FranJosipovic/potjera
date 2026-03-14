package com.fran.dev.potjera.android.app.game.models.dto.board

data class PlayerVHunterBoardStateDto(
    val questionsStarted: Boolean,
    val boardQuestion: BoardQuestionDto?,
    val hunterAnswer: String?,
    val playerAnswer: String?,
    val hunterCorrectAnswers: Int,
    val playerCorrectAnswers: Int,
    val playerStartingIndex: Int,
    val moneyInGame: Float,
    val boardPhase: String  // maps to BoardPhase enum
)
