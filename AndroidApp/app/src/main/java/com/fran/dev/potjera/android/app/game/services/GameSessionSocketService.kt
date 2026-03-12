package com.fran.dev.potjera.android.app.game.services

import android.util.Log
import com.fran.dev.potjera.android.app.game.models.dto.CoinBoosterStartHunterDto
import com.fran.dev.potjera.android.app.game.models.dto.CoinBoosterStartPlayerDto
import com.fran.dev.potjera.android.app.game.models.dto.board.AnswerRevealDto
import com.fran.dev.potjera.android.app.game.models.dto.board.BoardPhaseFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.board.HunterAnsweredQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.MoneyOfferAcceptedDto
import com.fran.dev.potjera.android.app.game.models.dto.board.MoneyOfferDto
import com.fran.dev.potjera.android.app.game.models.dto.board.NewBoardQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerAnsweredQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerCaughtDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerWonDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader

@Singleton
class GameSessionSocketService @Inject constructor() {

    companion object {
        private const val TAG = "GameSessionSocketService"
    }

    private var stompClient: StompClient? = null
    private val _events = MutableSharedFlow<GameSessionSocketEvent>()
    val events: SharedFlow<GameSessionSocketEvent> = _events.asSharedFlow()
    private val compositeDisposable = CompositeDisposable()
    private val gson = Gson()

    fun connect(gameSessionId: String, token: String) {

        Log.d(TAG, "connect to ws: $gameSessionId, $token")

        stompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            "ws://10.0.2.2:8080/ws/websocket"
        )

        stompClient!!.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                when (event.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d(TAG, "Connected")

                        subscribeToGameSession(gameSessionId)   // /topic/...
                        subscribeToUserQueue {
                            sendConnect(gameSessionId)
                        }
                    }

                    LifecycleEvent.Type.ERROR -> Log.e(
                        TAG,
                        "Error: ${event.exception}"
                    )

                    LifecycleEvent.Type.CLOSED -> Log.d(TAG, "Disconnected")
                    else -> {}
                }
            }.also { compositeDisposable.add(it) }

        stompClient!!.connect(
            listOf(
                StompHeader("Authorization", "Bearer $token")
            )
        )
    }

    private fun subscribeToGameSession(gameSessionId: String) {
        stompClient!!.topic("/topic/game-session/$gameSessionId")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { message ->
                Log.d("GameSessionSocket", "Received: ${message.payload}")
                val event = gson.fromJson(message.payload, GameSessionEventDto::class.java)
                handleEvent(event)
            }.also { compositeDisposable.add(it) }
    }

    private fun subscribeToUserQueue(onSubscribed: () -> Unit) {
        val disposable = stompClient!!
            .topic("/user/queue/game-session")
            .doOnSubscribe {
                Log.d(TAG, "Subscribed to user queue")
                onSubscribed()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { message ->
                Log.d("GameSessionSocket", "User queue message: ${message.payload}")
                val event = gson.fromJson(message.payload, GameSessionEventDto::class.java)
                handlePrivateMessage(event)
            }

        compositeDisposable.add(disposable)
    }

    private fun handleEvent(event: GameSessionEventDto) {
        val socketEvent: GameSessionSocketEvent = when (event.type) {

            "COIN_BOOSTER_FINISHED" -> {
                val dto: CoinBoosterFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), CoinBoosterFinishedDto::class.java
                )
                GameSessionSocketEvent.CoinBoosterFinishedEvent(dto)
            }

            "GAME_FINISHED" -> {
                val type = object : TypeToken<List<GameResultDto>>() {}.type
                val results: List<GameResultDto> = gson.fromJson(gson.toJson(event.payload), type)
                GameSessionSocketEvent.GameFinishedEvent(results)
            }

            "BOARD_PHASE_STARTING", "NEXT_PLAYER" -> {
                val dto: BoardPhaseStartingDto = gson.fromJson(
                    gson.toJson(event.payload), BoardPhaseStartingDto::class.java
                )
                GameSessionSocketEvent.BoardPhaseStartingEvent(dto)
            }

            "MONEY_OFFER" -> {
                val dto: MoneyOfferDto = gson.fromJson(
                    gson.toJson(event.payload), MoneyOfferDto::class.java
                )
                GameSessionSocketEvent.MoneyOfferEvent(dto)
            }

            "NEW_BOARD_QUESTION" -> {
                val dto: NewBoardQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), NewBoardQuestionDto::class.java
                )
                GameSessionSocketEvent.NewBoardQuestionEvent(dto)
            }

            "PLAYER_ANSWERED_BOARD_QUESTION_RES" -> {
                val dto: PlayerAnsweredQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerAnsweredQuestionDto::class.java
                )
                GameSessionSocketEvent.PlayerAnsweredQuestionEvent(dto)
            }

            "HUNTER_ANSWERED_BOARD_QUESTION_RES" -> {
                val dto: HunterAnsweredQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweredQuestionDto::class.java
                )
                GameSessionSocketEvent.HunterAnsweredQuestionEvent(dto)
            }

            "MONEY_OFFER_ACCEPTED" -> {
                val dto: MoneyOfferAcceptedDto = gson.fromJson(
                    gson.toJson(event.payload), MoneyOfferAcceptedDto::class.java
                )
                GameSessionSocketEvent.MoneyOfferAcceptedEvent(dto)
            }

            "BOARD_QUESTION_REVEAL" -> {
                val dto: AnswerRevealDto = gson.fromJson(
                    gson.toJson(event.payload), AnswerRevealDto::class.java
                )
                GameSessionSocketEvent.AnswerRevealedEvent(dto)
            }

            "PLAYER_WON" -> {
                val dto: PlayerWonDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerWonDto::class.java
                )
                GameSessionSocketEvent.PlayerWonEvent(dto)
            }

            "PLAYER_CAUGHT" -> {
                val dto: PlayerCaughtDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerCaughtDto::class.java
                )
                GameSessionSocketEvent.PlayerCaughtEvent(dto)
            }

            "BOARD_PHASE_FINISHED" -> {
                val dto: BoardPhaseFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), BoardPhaseFinishedDto::class.java
                )
                GameSessionSocketEvent.BoardPhaseFinishedEvent(dto)
            }

            "PLAYERS_ANSWERING_START" -> {
                val dto: PlayersAnsweringStartDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringStartDto::class.java
                )
                GameSessionSocketEvent.PlayersAnsweringPhaseStartEvent(dto)
            }

            "PLAYER_SIGNED_IN" -> {
                val dto: PlayerSignedInDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerSignedInDto::class.java
                )
                GameSessionSocketEvent.PlayerBuzzedInEvent(dto)
            }

            "PLAYERS_ANSWERING_CORRECT" -> {
                val dto: PlayersAnsweringCorrectDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringCorrectDto::class.java
                )
                GameSessionSocketEvent.PlayersAnsweringCorrectEvent(dto)
            }

            "PLAYERS_ANSWERING_WRONG" -> {
                val dto: PlayersAnsweringWrongDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringWrongDto::class.java
                )
                GameSessionSocketEvent.PlayersAnsweringWrongEvent(dto)
            }

            "PLAYERS_ANSWERING_NEXT_QUESTION" -> {
                val dto: PlayersAnsweringNextQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringNextQuestionDto::class.java
                )
                GameSessionSocketEvent.PlayersAnsweringNextQuestionEvent(dto)
            }

            "PLAYERS_ANSWERING_FINISHED" -> {
                val dto: PlayersAnsweringFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringFinishedDto::class.java
                )
                GameSessionSocketEvent.PlayersAnsweringPhaseFinishedEvent(dto)
            }

            "HUNTER_ANSWERING_START" -> {
                val dto: HunterAnsweringPhaseStartDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweringPhaseStartDto::class.java
                )
                GameSessionSocketEvent.HunterAnsweringPhaseStartEvent(dto)
            }

            "HUNTER_ANSWERING_CORRECT" -> {
                val dto: HunterAnsweredCorrectDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweredCorrectDto::class.java
                )
                GameSessionSocketEvent.HunterAnsweredCorrectEvent(dto)
            }

            "HUNTER_ANSWERING_WRONG" -> {
                val dto: HunterAnsweredWrongDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweredWrongDto::class.java
                )
                GameSessionSocketEvent.HunterAnsweredWrongEvent(dto)
            }

            "PLAYER_COUNTER_CORRECT" -> {
                val dto: PlayerCounterAnswerCorrectDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerCounterAnswerCorrectDto::class.java
                )
                GameSessionSocketEvent.PlayerCounterAnswerCorrectEvent(dto)
            }

            "PLAYER_COUNTER_WRONG" -> {
                val dto: PlayerCounterAnswerWrongDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerCounterAnswerWrongDto::class.java
                )
                GameSessionSocketEvent.PlayerCounterAnswerWrongEvent(dto)
            }

            "HUNTER_ANSWERING_NEXT_QUESTION" -> {
                val dto: HunterAnsweringNextQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweringNextQuestionDto::class.java
                )
                GameSessionSocketEvent.HunterAnsweringNextQuestionEvent(dto)
            }

            "SUGGESTION" -> {
                val dto: HunterAnsweringPhaseSuggestionDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweringPhaseSuggestionDto::class.java
                )
                GameSessionSocketEvent.HunterAnsweringSuggestionEvent(dto)
            }

            "HUNTER_ANSWERING_FINISHED" -> {
                val dto: HunterAnsweringPhaseFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweringPhaseFinishedDto::class.java
                )
                GameSessionSocketEvent.HunterAnsweringPhaseFinishedEvent(dto)
            }

            "HUNTER_TIMER_PAUSED" -> {
                val dto: HunterTimerPausedDto = gson.fromJson(
                    gson.toJson(event.payload), HunterTimerPausedDto::class.java
                )
                GameSessionSocketEvent.HunterTimerPausedEvent(dto)
            }

            "HUNTER_TIMER_RESUMED" -> {
                val dto: HunterTimerResumedDto = gson.fromJson(
                    gson.toJson(event.payload), HunterTimerResumedDto::class.java
                )
                GameSessionSocketEvent.HunterTimerResumedEvent(dto)
            }

            else -> {
                Log.w(TAG, "Unknown event type: ${event.type}")
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch { _events.emit(socketEvent) }
    }

    private fun handlePrivateMessage(event: GameSessionEventDto) {
        Log.d(TAG, "handlePrivateMessage: ${event.payload}")
        val socketEvent: GameSessionSocketEvent = when (event.type) {
            "COIN_BOOSTER_START_HUNTER" -> {
                val dto: CoinBoosterStartHunterDto = gson.fromJson(
                    gson.toJson(event.payload), CoinBoosterStartHunterDto::class.java
                )
                Log.d(
                    TAG,
                    "handlePrivateMessage: COIN_BOOSTER_START  payload: ${event.payload}, parse: $dto"
                )
                GameSessionSocketEvent.CoinBoosterStartedHunterEvent(dto)
            }

            "COIN_BOOSTER_START_PLAYER" -> {
                val dto: CoinBoosterStartPlayerDto = gson.fromJson(
                    gson.toJson(event.payload), CoinBoosterStartPlayerDto::class.java
                )
                Log.d(
                    TAG,
                    "handlePrivateMessage: COIN_BOOSTER_START  payload: ${event.payload}, parse: $dto"
                )
                GameSessionSocketEvent.CoinBoosterStartedPlayerEvent(dto)
            }

            else -> {
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            _events.emit(socketEvent)
        }
    }

    // ── Send messages to server ──────────────────────────────────────────────

    fun sendMoneyOffer(gameSessionId: String, higherOffer: Float, lowerOffer: Float) {
        val payload = gson.toJson(MoneyOfferPayload(higherOffer, lowerOffer))
        send(gameSessionId, "money-offer-request", payload)
    }

    fun sendMoneyOfferResponse(gameSessionId: String, acceptedOffer: Float) {
        val payload = gson.toJson(MoneyOfferResponsePayload(acceptedOffer))
        send(gameSessionId, "money-offer-response", payload)
    }

    fun sendBoardAnswer(gameSessionId: String, answer: String, isHunter: Boolean) {
        val payload = gson.toJson(BoardAnswerPayload(answer))
        val endpoint =
            if (isHunter) "hunter-answer-board-question" else "player-answer-board-question"
        send(gameSessionId, endpoint, payload)
    }

    fun sendStartBoardQuestions(gameSessionId: String) {
        send(gameSessionId, "start-board-phase", "{}")
    }

    fun sendFinish(gameSessionId: String, correctAnswers: Int) {
        val payload = gson.toJson(FinishCoinBoosterPayload(correctAnswers))
        send(gameSessionId, "finish-coin-booster", payload)
    }

    fun sendConnect(gameSessionId: String) {
        send(gameSessionId, "connect", "{}")
    }

    fun sendBuzzIn(gameSessionId: String) {
        send(gameSessionId, "players-answering/buzz-in", "{}")
    }

    fun sendPlayersAnsweringAnswer(gameSessionId: String, answer: String) {
        val payload = gson.toJson(PlayersAnsweringAnswerPayload(answer))
        send(gameSessionId, "players-answering/answer", payload)
    }

    fun sendHunterAnsweringAnswer(gameSessionId: String, answer: String) {
        val payload = gson.toJson(HunterAnsweringHunterAnswerPayload(answer))
        send(gameSessionId, "hunter-answering/answer", payload)
    }

    fun sendPlayerCounterAnswer(gameSessionId: String, answer: String) {
        val payload = gson.toJson(PlayersAnsweringCounterAnswerPayload(answer))
        send(gameSessionId, "hunter-answering/counter-answer", payload)
    }

    fun sendSuggestion(gameSessionId: String, suggestion: String){
        val payload = gson.toJson(HunterAnsweringPhaseSuggestionPayload(suggestion))
        send(gameSessionId, "hunter-answering/suggestion", payload)
    }


    // ── private helper ────────────────────────────────────────────────────────────
    private fun send(gameSessionId: String, endpoint: String, payload: String) {
        stompClient?.send("/app/game-session/$gameSessionId/$endpoint", payload)
            ?.subscribeOn(Schedulers.io())
            ?.subscribe(
                { Log.d(TAG, "$endpoint sent") },
                { Log.e(TAG, "Failed to send $endpoint: ${it.message}") }
            )
            ?.also { compositeDisposable.add(it) }
    }

    fun disconnect() {
        compositeDisposable.clear()
        stompClient?.disconnect()
    }
}

data class HunterAnsweringHunterAnswerPayload(val answer: String)
data class PlayersAnsweringCounterAnswerPayload(val answer: String)


// ── Events ───────────────────────────────────────────────────────────────────

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

    data class NewBoardQuestionEvent(val dto: NewBoardQuestionDto) : GameSessionSocketEvent()

    data class HunterAnsweredQuestionEvent(val dto: HunterAnsweredQuestionDto) :
        GameSessionSocketEvent()

    data class PlayerAnsweredQuestionEvent(val dto: PlayerAnsweredQuestionDto) :
        GameSessionSocketEvent()

    data class AnswerRevealedEvent(val dto: AnswerRevealDto) : GameSessionSocketEvent()
    data class PlayerWonEvent(val dto: PlayerWonDto) : GameSessionSocketEvent()
    data class PlayerCaughtEvent(val dto: PlayerCaughtDto) : GameSessionSocketEvent()
    data class BoardPhaseFinishedEvent(val dto: BoardPhaseFinishedDto) :
        GameSessionSocketEvent()

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

// ── Received from server (Dto) ────────────────────────────────────────────

data class HunterTimerPausedDto(
    val remainingMs: Long
)
data class HunterTimerResumedDto(
    val endTimestamp: Long
)

data class GameSessionEventDto(val type: String, val payload: Any)

data class HunterAnsweringPhaseSuggestionDto(
    val sentBy: Long,
    val suggestion: String,
    val username: String
)

data class CoinBoosterFinishedDto(
    val playerId: Long,
    val username: String,
    val moneyWon: Float
)

data class GameResultDto(
    val playerId: Long,
    val correctAnswers: Int
)

// board phase — now matches BoardPhaseStartingPayload(globalState, boardState)
data class BoardPhaseStartingDto(
    val currentPlayerId: Long,
    val boardState: PlayerVHunterBoardStateDto
)

data class PlayerVHunterGlobalStateDto(
    val hunterId: Long,
    val currentPlayerId: Long,
    val playersFinishStatus: Map<Long, Float>,
    val players: Map<Long, String>
)

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

data class BoardQuestionDto(
    val question: String,
    val choices: List<String>,
    val correctAnswer: String
)


// PlayersAnsweringPhase
data class PlayersAnsweringStateDto(
    val correctAnswers: Int = 0,
    val signedPlayerId: Long? = null,
    val currentQuestionIndex: Int = 0
)

data class PlayersAnsweringQuestion(
    val question: String,
    val answer: String,
    val aliases: List<String>
)

data class PlayersAnsweringStartDto(
    val playersAnsweringState: PlayersAnsweringStateDto,
    val question: PlayersAnsweringQuestion,
    val questionNum: Int
)

// PLAYER_SIGNED_IN
data class PlayerSignedInDto(
    val playerId: Long
)

// PLAYERS_ANSWERING_CORRECT
data class PlayersAnsweringCorrectDto(
    val playerId: Long,
    val correctAnswer: String
)

// PLAYERS_ANSWERING_WRONG
data class PlayersAnsweringWrongDto(
    val playerId: Long,
    val correctAnswer: String
)

// PLAYERS_ANSWERING_NEXT_QUESTION
data class PlayersAnsweringNextQuestionDto(
    val question: PlayersAnsweringQuestion,
    val questionNum: Int,
    val total: Int
)

// PLAYERS_ANSWERING_FINISHED
data class PlayersAnsweringFinishedDto(
    val correctAnswers: Int,
    val playerIds: List<Long>
)

//Hunter phase
data class HunterAnsweringStateDto(
    val hunterCorrectAnswers: Int = 0,
    val totalStepsToReach: Int = 0,
    val currentQuestionIndex: Int = 0,
    val hunterJustWrong: Boolean = false
)

class PlayersAnsweringQuestionDto {
    var question: String? = null
    var answer: String? = null
    var aliases: MutableList<String?>? = null
}

data class HunterAnsweringPhaseStartDto(
    val hunterAnsweringState: HunterAnsweringStateDto,
    val question: PlayersAnsweringQuestionDto,
    val endTimestamp: Long
)

data class HunterAnsweredCorrectDto(
    val correctAnswer: String
)

data class HunterAnsweredWrongDto(
    val correctAnswer: String
)

data class PlayerCounterAnswerCorrectDto(
    val correctAnswer: String
)

data class PlayerCounterAnswerWrongDto(
    val correctAnswer: String
)

data class HunterAnsweringNextQuestionDto(
    val question: String
)

data class HunterAnsweringPhaseFinishedDto(
    val hunterWon: Boolean,
)

// ── Sent to server (Payload) ──────────────────────────────────────────────

data class MoneyOfferPayload(val higherOffer: Float, val lowerOffer: Float)
data class MoneyOfferResponsePayload(val offerAccepted: Float)
data class BoardAnswerPayload(val answer: String)
data class FinishCoinBoosterPayload(val correctAnswers: Int)
data class PlayersAnsweringAnswerPayload(val answer: String)

data class HunterAnsweringPhaseSuggestionPayload(val suggestion: String)

// ── Helpers ────────────────────────────────────────────────────────────────