package com.fran.dev.potjera.android.app.game.models.state

import com.fran.dev.potjera.android.app.game.models.BoardQuestion
import com.fran.dev.potjera.android.app.game.models.enums.BoardPhase

data class PlayerVHunterBoardState(
    var questionsStarted: Boolean = false,
    var boardQuestion: BoardQuestion? = null,
    var hunterAnswer: String? = null,
    var playerAnswer: String? = null,
    var hunterCorrectAnswers: Int = 0,
    var playerCorrectAnswers: Int = 0,
    var playerStartingIndex: Int = 2,
    var moneyInGame: Float = 0f,
    var boardPhase: BoardPhase = BoardPhase.HUNTER_MAKING_OFFER
)
