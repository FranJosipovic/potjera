package com.fran.dev.potjera.android.app.room.presentation.lobby

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.room.api.RoomDetailsResponse
import com.fran.dev.potjera.android.app.room.api.RoomPlayerDTO
import com.fran.dev.potjera.android.app.room.repository.RoomRepository
import com.fran.dev.potjera.android.app.room.repository.RoomResult
import com.fran.dev.potjera.android.app.room.services.RoomSocketEvent
import com.fran.dev.potjera.android.app.room.services.RoomSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomLobbyViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val socketService: RoomSocketService,
    private val prefs: SharedPreferences
) : ViewModel() {

    companion object{
        private const val TAG = "RoomLobbyViewModel"
    }

    private val myUserId = prefs.getLong("user_id", 0L)

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _isStartingGame = MutableStateFlow(false)
    val isStartingGame: StateFlow<Boolean> = _isStartingGame.asStateFlow()

    private val _roomDetails = MutableStateFlow<RoomDetailsResponse?>(null)
    val roomDetails: StateFlow<RoomDetailsResponse?> = _roomDetails.asStateFlow()

    private val _players = MutableStateFlow<List<RoomPlayerDTO>>(emptyList())
    val players: StateFlow<List<RoomPlayerDTO>> = _players.asStateFlow()

    private val _hunter = MutableStateFlow<RoomPlayerDTO?>(null)
    val hunter: StateFlow<RoomPlayerDTO?> = _hunter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var roomId: String = ""
    fun initRoom(roomId: String) {
        this.roomId = roomId
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = roomRepository.getRoomDetails(roomId)) {
                is RoomResult.Success<*> -> {
                    val resData = result.data as RoomDetailsResponse
                    _roomDetails.value = resData
                    _players.value = resData.players
                    _hunter.value = resData.hunter

                    // check if current user is host
                    _isHost.value = resData.players
                        .any { it.playerId == myUserId && it.isHost }
                }

                else -> {}
            }
            _isLoading.value = false
            connectToRoom(roomId)
        }
    }

    fun startGame(roomId: String) {
        viewModelScope.launch {
            _isStartingGame.value = true
            when (val res = roomRepository.startGame(roomId)) {
                is RoomResult.Success -> {
                    // game started — wait for GAME_STARTING ws event
                    // which will trigger navigation via the events channel
                }

                is RoomResult.Forbidden -> {
                    // not host — shouldn't happen since button is hidden
                }

                else -> {
                    // show error if needed
                }
            }
            _isStartingGame.value = false
        }
    }

    private fun connectToRoom(roomId: String) {
        socketService.connect(roomId, prefs.getString("token", "")!!)

        viewModelScope.launch {
            socketService.events.collect { event ->
                when (event) {
                    is RoomSocketEvent.GameStarting -> {
                        _events.send(GameEvent.StartGame(event.payload.gameSessionId))
                    }

                    is RoomSocketEvent.HunterChanged -> {

                        Log.d(TAG, "HunterChanged: ${event.payload}")

                        _players.update { current ->
                            current.map { it.copy(isHunter = it.playerId == event.payload.playerId) }
                        }
                        _hunter.value =
                            _players.value.find { it.playerId == event.payload.playerId }
                    }

                    is RoomSocketEvent.PlayerJoined -> {
                        val player = RoomPlayerDTO(
                            playerId = event.player.playerId,
                            username = event.player.username,
                            rank = event.player.rank,
                            isHost = false,
                            isReady = event.player.isReady,
                            isHunter = event.player.isHunter
                        )

                        if (event.player.isHunter) {
                            // remove hunter flag from previous hunter
                            _players.update { current ->
                                current.map { it.copy(isHunter = false) }
                            }
                            _hunter.value = player
                        }

                        _players.update { current -> current + player }
                    }

                    is RoomSocketEvent.PlayerLeftRoom -> {
                        _players.update { current ->
                            current.filter { it.playerId != event.payLoad.playerId }
                        }

                        event.payLoad.newHunterId?.let { newHunterId ->
                            _players.update { current ->
                                current.map { it.copy(isHunter = it.playerId == newHunterId) }
                            }
                            _hunter.value = _players.value.find { it.playerId == newHunterId }
                        }
                    }

                    is RoomSocketEvent.RoomClosed -> {
                        _events.send(GameEvent.RoomClosed)
                    }
                }
            }
        }
    }

    fun assignHunter(roomId: String, hunterId: Long) {
        viewModelScope.launch {
            roomRepository.assignHunter(roomId, hunterId)
        }
    }

    fun leaveRoom(roomId: String) {
        viewModelScope.launch {
            roomRepository.leaveRoom(roomId)
        }
    }

    private val _events = Channel<GameEvent>()
    val events = _events.receiveAsFlow()

    override fun onCleared() {
        super.onCleared()
        socketService.disconnect()
    }
}

sealed class GameEvent {
    data class StartGame(val gameSessionId: String) : GameEvent()
    object RoomClosed : GameEvent()
}