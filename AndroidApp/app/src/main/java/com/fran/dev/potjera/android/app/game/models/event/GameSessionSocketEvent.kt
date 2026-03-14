package com.fran.dev.potjera.android.app.game.models.event

import com.fran.dev.potjera.android.app.game.models.dto.CoinBoosterStartHunterDto
import com.fran.dev.potjera.android.app.game.models.dto.CoinBoosterStartPlayerDto
import com.fran.dev.potjera.android.app.game.models.dto.board.AnswerRevealDto
import com.fran.dev.potjera.android.app.game.models.dto.board.BoardPhaseFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.board.BoardPhaseStartingDto
import com.fran.dev.potjera.android.app.game.models.dto.board.BoardQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.HunterAnsweredQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.MoneyOfferAcceptedDto
import com.fran.dev.potjera.android.app.game.models.dto.board.MoneyOfferDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerAnsweredQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerCaughtDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerWonDto
import com.fran.dev.potjera.android.app.game.models.dto.coinbooster.CoinBoosterFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.game.GameResultDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweredCorrectDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweredWrongDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringNextQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringPhaseFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringPhaseStartDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringPhaseSuggestionDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterTimerPausedDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterTimerResumedDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.PlayerCounterAnswerCorrectDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.PlayerCounterAnswerWrongDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayerSignedInDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringCorrectDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringNextQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringStartDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringWrongDto

sealed class GameSessionSocketEvent {
    data class CoinBoosterStartedHunterEvent(val dto: CoinBoosterStartHunterDto) :
        GameSessionSocketEvent()

    data class CoinBoosterStartedPlayerEvent(val dto: CoinBoosterStartPlayerDto) :
        GameSessionSocketEvent()

    data class CoinBoosterFinishedEvent(val payload: CoinBoosterFinishedDto) :
        GameSessionSocketEvent()

    data class GameFinishedEvent(val results: List<GameResultDto>) : GameSessionSocketEvent()
    data class PlayerLeftEvent(val playerId: Long) : GameSessionSocketEvent()

    // board phase — BOARD_PHASE_STARTING and NEXT_PLAYER share same shape
    data class BoardPhaseStartingEvent(val dto: BoardPhaseStartingDto) : GameSessionSocketEvent()
    data class MoneyOfferEvent(val dto: MoneyOfferDto) : GameSessionSocketEvent()
    data class MoneyOfferAcceptedEvent(val dto: MoneyOfferAcceptedDto) : GameSessionSocketEvent()

    data class NewBoardQuestionEvent(val dto: BoardQuestionDto) : GameSessionSocketEvent()

    data class HunterAnsweredQuestionEvent(val dto: HunterAnsweredQuestionDto) :
        GameSessionSocketEvent()

    data class PlayerAnsweredQuestionEvent(val dto: PlayerAnsweredQuestionDto) :
        GameSessionSocketEvent()

    data class AnswerRevealedEvent(val dto: AnswerRevealDto) : GameSessionSocketEvent()
    data class PlayerWonEvent(val dto: PlayerWonDto) : GameSessionSocketEvent()
    data class PlayerCaughtEvent(val dto: PlayerCaughtDto) : GameSessionSocketEvent()
    data class BoardPhaseFinishedEvent(val dto: BoardPhaseFinishedDto) : GameSessionSocketEvent()

    // players answering phase
    data class PlayersAnsweringPhaseStartEvent(val dto: PlayersAnsweringStartDto) :
        GameSessionSocketEvent()

    data class PlayerBuzzedInEvent(val dto: PlayerSignedInDto) : GameSessionSocketEvent()
    data class PlayersAnsweringCorrectEvent(val dto: PlayersAnsweringCorrectDto) :
        GameSessionSocketEvent()

    data class PlayersAnsweringWrongEvent(val dto: PlayersAnsweringWrongDto) :
        GameSessionSocketEvent()

    data class PlayersAnsweringNextQuestionEvent(val dto: PlayersAnsweringNextQuestionDto) :
        GameSessionSocketEvent()

    data class PlayersAnsweringPhaseFinishedEvent(val dto: PlayersAnsweringFinishedDto) :
        GameSessionSocketEvent()

    // hunter answering phase
    data class HunterAnsweringPhaseStartEvent(val dto: HunterAnsweringPhaseStartDto) :
        GameSessionSocketEvent()

    data class HunterAnsweredCorrectEvent(val dto: HunterAnsweredCorrectDto) :
        GameSessionSocketEvent()

    data class HunterAnsweredWrongEvent(val dto: HunterAnsweredWrongDto) : GameSessionSocketEvent()

    data class PlayerCounterAnswerCorrectEvent(val dto: PlayerCounterAnswerCorrectDto) :
        GameSessionSocketEvent()

    data class PlayerCounterAnswerWrongEvent(val dto: PlayerCounterAnswerWrongDto) :
        GameSessionSocketEvent()

    data class HunterAnsweringNextQuestionEvent(val dto: HunterAnsweringNextQuestionDto) :
        GameSessionSocketEvent()

    data class HunterAnsweringPhaseFinishedEvent(val dto: HunterAnsweringPhaseFinishedDto) :
        GameSessionSocketEvent()

    data class HunterAnsweringSuggestionEvent(val dto: HunterAnsweringPhaseSuggestionDto) :
        GameSessionSocketEvent()

    data class HunterTimerPausedEvent(val dto: HunterTimerPausedDto) : GameSessionSocketEvent()
    data class HunterTimerResumedEvent(val dto: HunterTimerResumedDto) : GameSessionSocketEvent()
}
