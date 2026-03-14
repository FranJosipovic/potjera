package com.fran.dev.potjera.android.app.game

import com.fran.dev.potjera.android.app.game.models.BoardQuestion
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerVHunterBoardStateDto
import com.fran.dev.potjera.android.app.game.models.enums.BoardPhase
import com.fran.dev.potjera.android.app.game.models.state.PlayerVHunterBoardState

fun PlayerVHunterBoardStateDto.toState(): PlayerVHunterBoardState {
    return PlayerVHunterBoardState(
        questionsStarted = questionsStarted,
        boardQuestion = boardQuestion?.let {
            BoardQuestion(
                question = it.question,
                choices = it.choices.toMutableList(),
                correctAnswer = it.correctAnswer
            )
        },
        hunterAnswer = hunterAnswer,
        playerAnswer = playerAnswer,
        hunterCorrectAnswers = hunterCorrectAnswers,
        playerCorrectAnswers = playerCorrectAnswers,
        playerStartingIndex = playerStartingIndex,
        moneyInGame = moneyInGame,
        boardPhase = BoardPhase.valueOf(boardPhase)
    )
}
