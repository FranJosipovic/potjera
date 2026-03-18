package com.fran.dev.potjera.android.app.game.services

import android.util.Log
import com.fran.dev.potjera.android.app.game.models.dto.CoinBoosterStartHunterDto
import com.fran.dev.potjera.android.app.game.models.dto.CoinBoosterStartPlayerDto
import com.fran.dev.potjera.android.app.game.models.dto.GameSessionEventDto
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
import com.fran.dev.potjera.android.app.game.models.event.GameSessionEvent
import com.fran.dev.potjera.android.app.game.models.payload.BoardAnswerPayload
import com.fran.dev.potjera.android.app.game.models.payload.FinishCoinBoosterPayload
import com.fran.dev.potjera.android.app.game.models.payload.HunterAnsweringAnswerPayload
import com.fran.dev.potjera.android.app.game.models.payload.HunterSuggestionPayload
import com.fran.dev.potjera.android.app.game.models.payload.MoneyOfferPayload
import com.fran.dev.potjera.android.app.game.models.payload.MoneyOfferResponsePayload
import com.fran.dev.potjera.android.app.game.models.payload.PlayerCounterAnswerPayload
import com.fran.dev.potjera.android.app.game.models.payload.PlayersAnsweringAnswerPayload
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
    private val _events = MutableSharedFlow<GameSessionEvent>()
    val events: SharedFlow<GameSessionEvent> = _events.asSharedFlow()
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
        val socketEvent: GameSessionEvent = when (event.type) {

            "COIN_BOOSTER_FINISHED" -> {
                val dto: CoinBoosterFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), CoinBoosterFinishedDto::class.java
                )
                GameSessionEvent.CoinBoosterFinishedEvent(dto)
            }

            "GAME_FINISHED" -> {
                val type = object : TypeToken<List<GameResultDto>>() {}.type
                val results: List<GameResultDto> = gson.fromJson(gson.toJson(event.payload), type)
                GameSessionEvent.GameFinishedEvent(results)
            }

            "BOARD_PHASE_STARTING", "NEXT_PLAYER" -> {
                val dto: BoardPhaseStartingDto = gson.fromJson(
                    gson.toJson(event.payload), BoardPhaseStartingDto::class.java
                )
                GameSessionEvent.BoardPhaseStartingEvent(dto)
            }

            "MONEY_OFFER" -> {
                val dto: MoneyOfferDto = gson.fromJson(
                    gson.toJson(event.payload), MoneyOfferDto::class.java
                )
                GameSessionEvent.MoneyOfferEvent(dto)
            }

            "NEW_BOARD_QUESTION" -> {
                val dto: BoardQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), BoardQuestionDto::class.java
                )
                GameSessionEvent.NewBoardQuestionEvent(dto)
            }

            "PLAYER_ANSWERED_BOARD_QUESTION_RES" -> {
                val dto: PlayerAnsweredQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerAnsweredQuestionDto::class.java
                )
                GameSessionEvent.PlayerAnsweredQuestionEvent(dto)
            }

            "HUNTER_ANSWERED_BOARD_QUESTION_RES" -> {
                val dto: HunterAnsweredQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweredQuestionDto::class.java
                )
                GameSessionEvent.HunterAnsweredQuestionEvent(dto)
            }

            "MONEY_OFFER_ACCEPTED" -> {
                val dto: MoneyOfferAcceptedDto = gson.fromJson(
                    gson.toJson(event.payload), MoneyOfferAcceptedDto::class.java
                )
                GameSessionEvent.MoneyOfferAcceptedEvent(dto)
            }

            "BOARD_QUESTION_REVEAL" -> {
                val dto: AnswerRevealDto = gson.fromJson(
                    gson.toJson(event.payload), AnswerRevealDto::class.java
                )
                GameSessionEvent.AnswerRevealedEvent(dto)
            }

            "PLAYER_WON" -> {
                val dto: PlayerWonDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerWonDto::class.java
                )
                GameSessionEvent.PlayerWonEvent(dto)
            }

            "PLAYER_CAUGHT" -> {
                val dto: PlayerCaughtDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerCaughtDto::class.java
                )
                GameSessionEvent.PlayerCaughtEvent(dto)
            }

            "BOARD_PHASE_FINISHED" -> {
                val dto: BoardPhaseFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), BoardPhaseFinishedDto::class.java
                )
                GameSessionEvent.BoardPhaseFinishedEvent(dto)
            }

            "PLAYERS_ANSWERING_START" -> {
                val dto: PlayersAnsweringStartDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringStartDto::class.java
                )
                GameSessionEvent.PlayersAnsweringPhaseStartEvent(dto)
            }

            "PLAYER_SIGNED_IN" -> {
                val dto: PlayerSignedInDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerSignedInDto::class.java
                )
                GameSessionEvent.PlayerBuzzedInEvent(dto)
            }

            "PLAYERS_ANSWERING_CORRECT" -> {
                val dto: PlayersAnsweringCorrectDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringCorrectDto::class.java
                )
                GameSessionEvent.PlayersAnsweringCorrectEvent(dto)
            }

            "PLAYERS_ANSWERING_WRONG" -> {
                val dto: PlayersAnsweringWrongDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringWrongDto::class.java
                )
                GameSessionEvent.PlayersAnsweringWrongEvent(dto)
            }

            "PLAYERS_ANSWERING_NEXT_QUESTION" -> {
                val dto: PlayersAnsweringNextQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringNextQuestionDto::class.java
                )
                GameSessionEvent.PlayersAnsweringNextQuestionEvent(dto)
            }

            "PLAYERS_ANSWERING_FINISHED" -> {
                val dto: PlayersAnsweringFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), PlayersAnsweringFinishedDto::class.java
                )
                GameSessionEvent.PlayersAnsweringPhaseFinishedEvent(dto)
            }

            "HUNTER_ANSWERING_START" -> {
                val dto: HunterAnsweringPhaseStartDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweringPhaseStartDto::class.java
                )
                GameSessionEvent.HunterAnsweringPhaseStartEvent(dto)
            }

            "HUNTER_ANSWERING_CORRECT" -> {
                val dto: HunterAnsweredCorrectDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweredCorrectDto::class.java
                )
                GameSessionEvent.HunterAnsweredCorrectEvent(dto)
            }

            "HUNTER_ANSWERING_WRONG" -> {
                val dto: HunterAnsweredWrongDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweredWrongDto::class.java
                )
                GameSessionEvent.HunterAnsweredWrongEvent(dto)
            }

            "PLAYER_COUNTER_CORRECT" -> {
                val dto: PlayerCounterAnswerCorrectDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerCounterAnswerCorrectDto::class.java
                )
                GameSessionEvent.PlayerCounterAnswerCorrectEvent(dto)
            }

            "PLAYER_COUNTER_WRONG" -> {
                val dto: PlayerCounterAnswerWrongDto = gson.fromJson(
                    gson.toJson(event.payload), PlayerCounterAnswerWrongDto::class.java
                )
                GameSessionEvent.PlayerCounterAnswerWrongEvent(dto)
            }

            "HUNTER_ANSWERING_NEXT_QUESTION" -> {
                val dto: HunterAnsweringNextQuestionDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweringNextQuestionDto::class.java
                )
                GameSessionEvent.HunterAnsweringNextQuestionEvent(dto)
            }

            "SUGGESTION" -> {
                val dto: HunterAnsweringPhaseSuggestionDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweringPhaseSuggestionDto::class.java
                )
                GameSessionEvent.HunterAnsweringSuggestionEvent(dto)
            }

            "HUNTER_ANSWERING_FINISHED" -> {
                val dto: HunterAnsweringPhaseFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), HunterAnsweringPhaseFinishedDto::class.java
                )
                GameSessionEvent.HunterAnsweringPhaseFinishedEvent(dto)
            }

            "HUNTER_TIMER_PAUSED" -> {
                val dto: HunterTimerPausedDto = gson.fromJson(
                    gson.toJson(event.payload), HunterTimerPausedDto::class.java
                )
                GameSessionEvent.HunterTimerPausedEvent(dto)
            }

            "HUNTER_TIMER_RESUMED" -> {
                val dto: HunterTimerResumedDto = gson.fromJson(
                    gson.toJson(event.payload), HunterTimerResumedDto::class.java
                )
                GameSessionEvent.HunterTimerResumedEvent(dto)
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
        val socketEvent: GameSessionEvent = when (event.type) {
            "COIN_BOOSTER_START_HUNTER" -> {
                val dto: CoinBoosterStartHunterDto = gson.fromJson(
                    gson.toJson(event.payload), CoinBoosterStartHunterDto::class.java
                )
                Log.d(
                    TAG,
                    "handlePrivateMessage: COIN_BOOSTER_START  payload: ${event.payload}, parse: $dto"
                )
                GameSessionEvent.CoinBoosterStartedHunterEvent(dto)
            }

            "COIN_BOOSTER_START_PLAYER" -> {
                val dto: CoinBoosterStartPlayerDto = gson.fromJson(
                    gson.toJson(event.payload), CoinBoosterStartPlayerDto::class.java
                )
                Log.d(
                    TAG,
                    "handlePrivateMessage: COIN_BOOSTER_START  payload: ${event.payload}, parse: $dto"
                )
                GameSessionEvent.CoinBoosterStartedPlayerEvent(dto)
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

    fun sendStartPlayersPhase(gameSessionId: String) {
        send(gameSessionId, "start-players-phase", "{}")
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
        val payload = gson.toJson(HunterAnsweringAnswerPayload(answer))
        send(gameSessionId, "hunter-answering/answer", payload)
    }

    fun sendPlayerCounterAnswer(gameSessionId: String, answer: String) {
        val payload = gson.toJson(PlayerCounterAnswerPayload(answer))
        send(gameSessionId, "hunter-answering/counter-answer", payload)
    }

    fun sendSuggestion(gameSessionId: String, suggestion: String) {
        val payload = gson.toJson(HunterSuggestionPayload(suggestion))
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
