package com.fran.dev.potjera.android.app.room.services

import android.util.Log
import com.google.gson.Gson
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
import kotlin.jvm.java

@Singleton
class RoomSocketService @Inject constructor() {

    private var stompClient: StompClient? = null
    private val _events = MutableSharedFlow<RoomSocketEvent>()
    val events: SharedFlow<RoomSocketEvent> = _events.asSharedFlow()

    private val compositeDisposable = CompositeDisposable()

    fun connect(roomId: String) {
        stompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            "ws://10.0.2.2:8080/ws/websocket" // SockJS endpoint
        )

        // connection lifecycle
        stompClient!!.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                when (event.type) {
                    LifecycleEvent.Type.OPENED  -> Log.d("STOMP", "Connected")
                    LifecycleEvent.Type.ERROR   -> Log.e("STOMP", "Error: ${event.exception}")
                    LifecycleEvent.Type.CLOSED  -> Log.d("STOMP", "Disconnected")
                    else -> {}
                }
            }.also { compositeDisposable.add(it) }

        // subscribe to room topic
        stompClient!!.topic("/topic/room/$roomId")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { message ->
                val event = Gson().fromJson(message.payload, RoomEventDto::class.java)
                handleEvent(event)
            }.also { compositeDisposable.add(it) }

        stompClient!!.connect()
    }

    private fun handleEvent(event: RoomEventDto) {
        val socketEvent = when (event.type) {
            "PLAYER_JOINED" -> {
                val payload = Gson().fromJson(
                    Gson().toJson(event.payload),
                    PlayerJoinedDto::class.java
                )
                RoomSocketEvent.PlayerJoined(payload)
            }
            "PLAYER_LEFT"   -> RoomSocketEvent.PlayerLeft(event.payload.toString())
            "GAME_STARTING" -> RoomSocketEvent.GameStarting
            else            -> return
        }
        // emit to flow
        CoroutineScope(Dispatchers.IO).launch {
            _events.emit(socketEvent)
        }
    }

    fun disconnect() {
        compositeDisposable.clear()
        stompClient?.disconnect()
    }
}

data class RoomEventDto(
    val type: String,
    val payload: Any
)

data class PlayerJoinedDto(
    val playerId: String,
    val username: String,
    val isHunter: Boolean,
    val rank: Int
)

sealed class RoomSocketEvent {
    data class PlayerJoined(val player: PlayerJoinedDto) : RoomSocketEvent()
    data class PlayerLeft(val playerId: String) : RoomSocketEvent()
    object GameStarting : RoomSocketEvent()
}