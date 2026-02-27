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
        Log.d(TAG, "handleEvent: ${event.payload}")
        val socketEvent: GameSessionSocketEvent = when (event.type) {
            "COIN_BOOSTER_START" -> {
                val type = object : TypeToken<CoinBoosterPlayerStateDto>() {}.type
                val payload: CoinBoosterPlayerStateDto = gson.fromJson(
                    gson.toJson(event.payload), type
                )
                GameSessionSocketEvent.CoinBoosterStarted(payload)
            }

            "PLAYER_FINISHED" -> {
                val type = object : TypeToken<PlayerFinishedDto>() {}.type
                val payload: PlayerFinishedDto = gson.fromJson(
                    gson.toJson(event.payload), type
                )
                GameSessionSocketEvent.PlayerFinished(payload)
            }

            "GAME_FINISHED" -> {
                val type = object : TypeToken<List<GameResultDto>>() {}.type
                val results: List<GameResultDto> = gson.fromJson(
                    gson.toJson(event.payload), type
                )
                GameSessionSocketEvent.GameFinished(results)
            }

            else -> {
                Log.w("GameSessionSocket", "Unknown event type: ${event.type}")
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            _events.emit(socketEvent)
        }
    }

    private fun handlePrivateMessage(event: GameSessionEventDto) {

        Log.d(TAG, "handlePrivateMessage: ${event.payload}")
        val socketEvent: GameSessionSocketEvent = when (event.type) {
            "COIN_BOOSTER_START" -> {
                val type = object : TypeToken<CoinBoosterPlayerStateDto>() {}.type
                val payload: CoinBoosterPlayerStateDto = gson.fromJson(
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

    // called when Game screen opens
    fun sendConnect(gameSessionId: String) {
        stompClient?.send("/app/game-session/$gameSessionId/connect", "{}")
            ?.subscribeOn(Schedulers.io())
            ?.subscribe(
                { Log.d("GameSessionSocket", "Connect sent") },
                { Log.e("GameSessionSocket", "Failed to send connect: ${it.message}") }
            )
            ?.also { compositeDisposable.add(it) }
    }

    // called when player finishes all questions
    fun sendFinish(gameSessionId: String, correctAnswers: Int) {
        val payload = gson.toJson(mapOf("correctAnswers" to correctAnswers))
        stompClient?.send("/app/game-session/$gameSessionId/finish", payload)
            ?.subscribeOn(Schedulers.io())
            ?.subscribe(
                { Log.d("GameSessionSocket", "Finish sent: correctAnswers=$correctAnswers") },
                { Log.e("GameSessionSocket", "Failed to send finish: ${it.message}") }
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

data class GameSessionEventDto(
    val type: String,
    val payload: Any
)

data class CoinBoosterPlayerStateDto(
    val playerId: Long,
    val isHunter: Boolean,
    val correctAnswers: Int,
    val questions: List<CoinBoosterQuestionDto>,
    val isFinished: Boolean
)

data class CoinBoosterQuestionDto(
    val question: String,
    val answer: String,
    val aliases: List<String>
)

// ── Events ───────────────────────────────────────────────────────────────────

sealed class GameSessionSocketEvent {
    data class CoinBoosterStarted(val playerState: CoinBoosterPlayerStateDto) :
        GameSessionSocketEvent()

    data class PlayerFinished(val payload: PlayerFinishedDto) : GameSessionSocketEvent()
    data class GameFinished(val results: List<GameResultDto>) : GameSessionSocketEvent()
    data class PlayerLeft(val playerId: Long) : GameSessionSocketEvent()
}

data class PlayerFinishedDto(val playerId: Long, val correctAnswers: Int)
data class GameResultDto(val playerId: Long, val correctAnswers: Int)