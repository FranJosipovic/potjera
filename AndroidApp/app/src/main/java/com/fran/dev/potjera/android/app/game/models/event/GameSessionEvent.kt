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

sealed class GameSessionEvent {
    data class CoinBoosterStartedHunterEvent(val dto: CoinBoosterStartHunterDto) :
        GameSessionEvent()

    data class CoinBoosterStartedPlayerEvent(val dto: CoinBoosterStartPlayerDto) :
        GameSessionEvent()

    data class CoinBoosterFinishedEvent(val payload: CoinBoosterFinishedDto) :
        GameSessionEvent()

    data class GameFinishedEvent(val results: List<GameResultDto>) : GameSessionEvent()
    data class PlayerLeftEvent(val playerId: Long) : GameSessionEvent()

    // board phase — BOARD_PHASE_STARTING and NEXT_PLAYER share same shape
    data class BoardPhaseStartingEvent(val dto: BoardPhaseStartingDto) : GameSessionEvent()
    data class MoneyOfferEvent(val dto: MoneyOfferDto) : GameSessionEvent()
    data class MoneyOfferAcceptedEvent(val dto: MoneyOfferAcceptedDto) : GameSessionEvent()

    data class NewBoardQuestionEvent(val dto: BoardQuestionDto) : GameSessionEvent()

    data class HunterAnsweredQuestionEvent(val dto: HunterAnsweredQuestionDto) :
        GameSessionEvent()

    data class PlayerAnsweredQuestionEvent(val dto: PlayerAnsweredQuestionDto) :
        GameSessionEvent()

    data class AnswerRevealedEvent(val dto: AnswerRevealDto) : GameSessionEvent()
    data class PlayerWonEvent(val dto: PlayerWonDto) : GameSessionEvent()
    data class PlayerCaughtEvent(val dto: PlayerCaughtDto) : GameSessionEvent()
    data class BoardPhaseFinishedEvent(val dto: BoardPhaseFinishedDto) : GameSessionEvent()

    // players answering phase
    data class PlayersAnsweringPhaseStartEvent(val dto: PlayersAnsweringStartDto) :
        GameSessionEvent()

    data class PlayerBuzzedInEvent(val dto: PlayerSignedInDto) : GameSessionEvent()
    data class PlayersAnsweringCorrectEvent(val dto: PlayersAnsweringCorrectDto) :
        GameSessionEvent()

    data class PlayersAnsweringWrongEvent(val dto: PlayersAnsweringWrongDto) :
        GameSessionEvent()

    data class PlayersAnsweringNextQuestionEvent(val dto: PlayersAnsweringNextQuestionDto) :
        GameSessionEvent()

    data class PlayersAnsweringPhaseFinishedEvent(val dto: PlayersAnsweringFinishedDto) :
        GameSessionEvent()

    // hunter answering phase
    data class HunterAnsweringPhaseStartEvent(val dto: HunterAnsweringPhaseStartDto) :
        GameSessionEvent()

    data class HunterAnsweredCorrectEvent(val dto: HunterAnsweredCorrectDto) :
        GameSessionEvent()

    data class HunterAnsweredWrongEvent(val dto: HunterAnsweredWrongDto) : GameSessionEvent()

    data class PlayerCounterAnswerCorrectEvent(val dto: PlayerCounterAnswerCorrectDto) :
        GameSessionEvent()

    data class PlayerCounterAnswerWrongEvent(val dto: PlayerCounterAnswerWrongDto) :
        GameSessionEvent()

    data class HunterAnsweringNextQuestionEvent(val dto: HunterAnsweringNextQuestionDto) :
        GameSessionEvent()

    data class HunterAnsweringPhaseFinishedEvent(val dto: HunterAnsweringPhaseFinishedDto) :
        GameSessionEvent()

    data class HunterAnsweringSuggestionEvent(val dto: HunterAnsweringPhaseSuggestionDto) :
        GameSessionEvent()

    data class HunterTimerPausedEvent(val dto: HunterTimerPausedDto) : GameSessionEvent()
    data class HunterTimerResumedEvent(val dto: HunterTimerResumedDto) : GameSessionEvent()
}
