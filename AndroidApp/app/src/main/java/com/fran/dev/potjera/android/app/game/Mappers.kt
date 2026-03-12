package com.fran.dev.potjera.android.app.game

import com.fran.dev.potjera.android.app.game.playervhunter.presentation.BoardPhase
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.BoardQuestion
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.PlayerVHunterBoardState
import com.fran.dev.potjera.android.app.game.services.PlayerVHunterBoardStateDto
import com.fran.dev.potjera.android.app.game.services.PlayerVHunterGlobalStateDto

fun PlayerVHunterBoardStateDto.toState(): PlayerVHunterBoardState {
    return PlayerVHunterBoardState(
        questionsStarted     = questionsStarted,
        boardQuestion        = boardQuestion?.let {
            BoardQuestion(
                question = it.question,
                choices = it.choices.toMutableList(),
                correctAnswer = it.correctAnswer
            )
        },
        hunterAnswer         = hunterAnswer,
        playerAnswer         = playerAnswer,
        hunterCorrectAnswers = hunterCorrectAnswers,
        playerCorrectAnswers = playerCorrectAnswers,
        playerStartingIndex  = playerStartingIndex,
        moneyInGame          = moneyInGame,
        boardPhase           = BoardPhase.valueOf(boardPhase)
    )
}
