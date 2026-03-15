package com.fran.dev.potjera.android.app.room.services

import android.util.Log
import com.fran.dev.potjera.android.app.room.model.CaptainChangedDto
import com.fran.dev.potjera.android.app.room.model.GameStartingDto
import com.fran.dev.potjera.android.app.room.model.HunterChangedDto
import com.fran.dev.potjera.android.app.room.model.PlayerJoinedDto
import com.fran.dev.potjera.android.app.room.model.PlayerLeftRoomDto
import com.fran.dev.potjera.android.app.room.model.RoomClosedDto
import com.fran.dev.potjera.android.app.room.model.RoomEventDto
import com.fran.dev.potjera.android.app.room.model.RoomSocketEvent
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
import ua.naiksoftware.stomp.dto.StompHeader
import kotlin.jvm.java

@Singleton
class RoomSocketService @Inject constructor() {

    companion object{
        private const val TAG = "RoomSocketService"
    }
    private val gson = Gson()
    private var stompClient: StompClient? = null
    private val _events = MutableSharedFlow<RoomSocketEvent>()
    val events: SharedFlow<RoomSocketEvent> = _events.asSharedFlow()

    private val compositeDisposable = CompositeDisposable()

    fun connect(roomId: String, token: String) {
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
                    LifecycleEvent.Type.OPENED -> Log.d("STOMP", "Connected")
                    LifecycleEvent.Type.ERROR -> Log.e("STOMP", "Error: ${event.exception}")
                    LifecycleEvent.Type.CLOSED -> Log.d("STOMP", "Disconnected")
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

        stompClient!!.connect(
            listOf(
                StompHeader("Authorization", "Bearer $token")
            )
        )
    }

    private fun handleEvent(event: RoomEventDto) {
        val socketEvent = when (event.type) {
            "PLAYER_JOINED" -> {
                val payload = gson.fromJson(
                    gson.toJson(event.payload),
                    PlayerJoinedDto::class.java
                )
                RoomSocketEvent.PlayerJoined(payload)
            }

            "GAME_STARTING" -> {
                val payload = gson.fromJson(  // ← fix: use gson.toJson first
                    gson.toJson(event.payload),
                    GameStartingDto::class.java
                )
                Log.d(TAG, "handleEvent GAME_STARTING: ${event.payload}, parse: $payload")
                RoomSocketEvent.GameStarting(payload)
            }

            "PLAYER_LEFT" -> {
                val payload = gson.fromJson(
                    gson.toJson(event.payload),
                    PlayerLeftRoomDto::class.java)
                RoomSocketEvent.PlayerLeftRoom(payload)
            }

            "HUNTER_CHANGED" -> {
                val payload = gson.fromJson(
                    gson.toJson(event.payload),
                    HunterChangedDto::class.java
                )
                RoomSocketEvent.HunterChanged(payload)
            }

            "CAPTAIN_CHANGED" -> {
                val payload = gson.fromJson(
                    gson.toJson(event.payload),
                    CaptainChangedDto::class.java
                )
                RoomSocketEvent.CaptainChanged(payload)
            }

            "ROOM_CLOSED" -> {
                val payload = gson.fromJson(
                    gson.toJson(event.payload),
                    RoomClosedDto::class.java
                )
                RoomSocketEvent.RoomClosed(payload)
            }

            else -> {
                Log.w("RoomSocket", "Unknown event: ${event.type}")
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            _events.emit(socketEvent)
        }
    }

    fun disconnect() {
        compositeDisposable.clear()
        stompClient?.disconnect()
    }
}
