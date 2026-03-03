package com.fran.dev.potjera.android.app.game.services

import android.util.Log
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
                GameSessionSocketEvent.CoinBoosterFinished(dto)
            }

            "GAME_FINISHED" -> {
                val type = object : TypeToken<List<GameResultDto>>() {}.type
                val results: List<GameResultDto> = gson.fromJson(gson.toJson(event.payload), type)
                GameSessionSocketEvent.GameFinished(results)
            }

            "BOARD_PHASE_STARTING", "NEXT_PLAYER" -> {
                val dto: BoardPhaseStartingDto = gson.fromJson(
                    gson.toJson(event.payload), BoardPhaseStartingDto::class.java
                )
                GameSessionSocketEvent.BoardPhaseStarting(dto)
            }

            "MONEY_OFFER" -> {
                val dto: MoneyOfferDto = gson.fromJson(
                    gson.toJson(event.payload), MoneyOfferDto::class.java
                )
                GameSessionSocketEvent.MoneyOffer(dto)
            }

            "NEW_BOARD_QUESTION",
            "PLAYER_ANSWERED_BOARD_QUESTION_RES",
            "HUNTER_ANSWERED_BOARD_QUESTION_RES"-> {
                val dto: PlayerVHunterBoardStateDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerVHunterBoardStateDto::class.java
                )
                // MONEY_OFFER_ACCEPTED also carries MoneyOfferAcceptedDto but we only
                // need the board state for the screen — emit as BoardQuestionUpdate
                GameSessionSocketEvent.BoardQuestionUpdate(dto)
            }

            "MONEY_OFFER_ACCEPTED"-> {
                val dto: PlayerVHunterBoardStateDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerVHunterBoardStateDto::class.java
                )
                // MONEY_OFFER_ACCEPTED also carries MoneyOfferAcceptedDto but we only
                // need the board state for the screen — emit as BoardQuestionUpdate
                GameSessionSocketEvent.MoneyOfferAccepted(dto)
            }

            "BOARD_QUESTION_REVEAL" -> {
                val dto: PlayerVHunterBoardStateDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerVHunterBoardStateDto::class.java
                )
                // MONEY_OFFER_ACCEPTED also carries MoneyOfferAcceptedDto but we only
                // need the board state for the screen — emit as BoardQuestionUpdate
                GameSessionSocketEvent.AnswerRevealed(dto)
            }

            "PLAYER_WON" -> {
                val dto: PlayerVHunterGlobalStateDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerVHunterGlobalStateDto::class.java
                )
                GameSessionSocketEvent.PlayerWon(dto)
            }

            "PLAYER_CAUGHT" -> {
                val dto: PlayerVHunterGlobalStateDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerVHunterGlobalStateDto::class.java
                )
                GameSessionSocketEvent.PlayerCaught(dto)
            }

            "BOARD_PHASE_FINISHED" -> {
                val dto: PlayerVHunterGlobalStateDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerVHunterGlobalStateDto::class.java
                )
                GameSessionSocketEvent.BoardPhaseFinished(dto)
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
            "COIN_BOOSTER_START" -> {
                val type = object : TypeToken<CoinBoosterStartPayload>() {}.type
                val payload: CoinBoosterStartPayload = gson.fromJson(
                    gson.toJson(event.payload), type
                )
                Log.d(
                    TAG,
                    "handlePrivateMessage: COIN_BOOSTER_START  payload: ${event.payload}, parse: $payload"
                )
                GameSessionSocketEvent.CoinBoosterStarted(payload)
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
        val endpoint = if (isHunter) "hunter-answer-board-question" else "player-answer-board-question"
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

// ── Payloads ─────────────────────────────────────────────────────────────────

data class AnswerPayload(
    val playerId: Long,
    val answer: String
)

// ── DTOs ─────────────────────────────────────────────────────────────────────

data class CurrentPlayerInfoDto(
    val playerId: Long,
    val correctAnswers: Int,
    val coinsEarned: Int
)


// ── Events ───────────────────────────────────────────────────────────────────

sealed class GameSessionSocketEvent {
    data class CoinBoosterStarted(val payload: CoinBoosterStartPayload) : GameSessionSocketEvent()
    data class CoinBoosterFinished(val payload: CoinBoosterFinishedDto) : GameSessionSocketEvent()
    data class GameFinished(val results: List<GameResultDto>) : GameSessionSocketEvent()
    data class PlayerLeft(val playerId: Long) : GameSessionSocketEvent()

    // board phase — BOARD_PHASE_STARTING and NEXT_PLAYER share same shape
    data class BoardPhaseStarting(val dto: BoardPhaseStartingDto) : GameSessionSocketEvent()
    data class MoneyOffer(val dto: MoneyOfferDto) : GameSessionSocketEvent()
    data class MoneyOfferAccepted(val dto: PlayerVHunterBoardStateDto) : GameSessionSocketEvent()
    data class BoardQuestionUpdate(val dto: PlayerVHunterBoardStateDto) : GameSessionSocketEvent()
    data class AnswerRevealed(val dto: PlayerVHunterBoardStateDto) : GameSessionSocketEvent()
    data class PlayerWon(val dto: PlayerVHunterGlobalStateDto) : GameSessionSocketEvent()
    data class PlayerCaught(val dto: PlayerVHunterGlobalStateDto) : GameSessionSocketEvent()
    data class BoardPhaseFinished(val dto: PlayerVHunterGlobalStateDto) : GameSessionSocketEvent()
}


// ── Received from server (Dto) ────────────────────────────────────────────

data class GameSessionEventDto(val type: String, val payload: Any)

data class CoinBoosterStartPayload(
    val playerState: CoinBoosterPlayerStateDto,
    val totalPlayersCount: Int
)

data class CoinBoosterPlayerStateDto(
    val playerId: Long,
    val isHunter: Boolean,
    val isHost: Boolean,
    val correctAnswers: Int,
    val questions: List<CoinBoosterQuestionDto>,
    val isFinished: Boolean
)

data class PlayerPlayingInfo(
    val playerId: Long,
    val username: String,
)

data class CoinBoosterQuestionDto(
    val question: String,
    val answer: String,
    val aliases: List<String>
)

data class CoinBoosterFinishedDto(
    val playerId: Long,
    val username: String,
    val correctAnswers: Int
)

data class GameResultDto(
    val playerId: Long,
    val correctAnswers: Int
)

// board phase — now matches BoardPhaseStartingPayload(globalState, boardState)
data class BoardPhaseStartingDto(
    val globalState: PlayerVHunterGlobalStateDto,
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

data class MoneyOfferDto(
    val higherOffer: Float,
    val lowerOffer: Float
)

data class MoneyOfferAcceptedDto(
    val playerId: Long,
    val acceptedOffer: Float,
    val playerStartingIndex: Int
)

data class PlayerWonDto(val playerId: Long, val moneyWon: Float)
data class PlayerCaughtDto(val playerId: Long)
data class NextPlayerDto(
    val globalState: PlayerVHunterGlobalStateDto,
    val boardState: PlayerVHunterBoardStateDto
)

// ── Sent to server (Payload) ──────────────────────────────────────────────

data class MoneyOfferPayload(val higherOffer: Float, val lowerOffer: Float)
data class MoneyOfferResponsePayload(val offerAccepted: Float)
data class BoardAnswerPayload(val answer: String)
data class FinishCoinBoosterPayload(val correctAnswers: Int)