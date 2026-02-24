package com.fran.dev.potjera.android.app.room.presentation.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.room.api.RoomDetailsResponse
import com.fran.dev.potjera.android.app.room.api.RoomPlayerDTO
import com.fran.dev.potjera.android.app.room.repository.RoomRepository
import com.fran.dev.potjera.android.app.room.repository.RoomResult
import com.fran.dev.potjera.android.app.room.services.RoomSocketEvent
import com.fran.dev.potjera.android.app.room.services.RoomSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Random
import javax.inject.Inject

@HiltViewModel
class RoomLobbyViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val socketService: RoomSocketService
) : ViewModel() {

    private val _roomDetails = MutableStateFlow<RoomDetailsResponse?>(null)
    val roomDetails: StateFlow<RoomDetailsResponse?> = _roomDetails.asStateFlow()

    private val _players = MutableStateFlow<List<RoomPlayerDTO>>(emptyList())
    val players: StateFlow<List<RoomPlayerDTO>> = _players.asStateFlow()

    private val _hunter = MutableStateFlow<RoomPlayerDTO?>(null)
    val hunter: StateFlow<RoomPlayerDTO?> = _hunter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun initRoom(roomId: String) {
        viewModelScope.launch {
            // 1. fetch room details
            _isLoading.value = true
            val result = roomRepository.getRoomDetails(roomId)

            when (result) {
                is RoomResult.Success<*> -> {
                    val resData = result.data as RoomDetailsResponse
                    _roomDetails.value = resData
                    _players.value = resData.players
                    _hunter.value = resData.hunter
                }
                is RoomResult.AlreadyInRoom -> {}
                is RoomResult.NotFound -> {}
                is RoomResult.RoomFull -> {}
                is RoomResult.UnknownError -> {}
            }

            _isLoading.value = false

            // 2. start WebSocket after data is loaded
            connectToRoom(roomId)
        }
    }

    private fun connectToRoom(roomId: String) {
        socketService.connect(roomId)

        viewModelScope.launch {
            socketService.events.collect { event ->
                when (event) {
                    is RoomSocketEvent.PlayerJoined -> {

                        val player = RoomPlayerDTO(
                            id       = "",
                            playerId = 0,
                            username = event.player.username,
                            rank     = event.player.rank,
                            isHost   = false,
                            isReady  = false,
                            isHunter = false
                        )

                        if(event.player.isHunter){
                            _hunter.value = player
                        }

                        _players.update { current ->
                            current + player
                        }
                    }
                    is RoomSocketEvent.PlayerLeft -> {
                        _players.update { current ->
                            current.filter { it.playerId.toString() != event.playerId }
                        }
                    }
                    is RoomSocketEvent.GameStarting -> { /* navigate */ }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketService.disconnect()
    }
}