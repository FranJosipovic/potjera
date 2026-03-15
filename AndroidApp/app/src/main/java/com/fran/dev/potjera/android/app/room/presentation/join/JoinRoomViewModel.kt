package com.fran.dev.potjera.android.app.room.presentation.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.room.model.RoomDetailsResponse
import com.fran.dev.potjera.android.app.room.repository.RoomRepository
import com.fran.dev.potjera.android.app.room.repository.RoomResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _publicRooms = MutableStateFlow<List<RoomDetailsResponse>>(emptyList())
    val publicRooms: StateFlow<List<RoomDetailsResponse>> = _publicRooms.asStateFlow()

    private val _searchResult = MutableStateFlow<RoomDetailsResponse?>(null)
    val searchResult: StateFlow<RoomDetailsResponse?> = _searchResult.asStateFlow()

    private val _isLoadingRooms = MutableStateFlow(false)
    val isLoadingRooms: StateFlow<Boolean> = _isLoadingRooms.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _error = MutableStateFlow<JoinRoomError?>(null)
    val error: StateFlow<JoinRoomError?> = _error.asStateFlow()

    init {
        loadPublicRooms()
    }

    fun loadPublicRooms() {
        viewModelScope.launch {
            _isLoadingRooms.value = true
            _error.value = null
            when (val result = roomRepository.getPublicRooms()) {
                is RoomResult.Success -> _publicRooms.value = result.data
                is RoomResult.UnknownError -> _error.value = JoinRoomError.LoadFailed
                else -> _error.value = JoinRoomError.LoadFailed
            }
            _isLoadingRooms.value = false
        }
    }

    fun searchByName(name: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _error.value = null
            _searchResult.value = null
            when (val result = roomRepository.searchByName(name)) {
                is RoomResult.Success -> _searchResult.value = result.data
                is RoomResult.NotFound -> _error.value = JoinRoomError.RoomNotFound
                else -> _error.value = JoinRoomError.UnknownError
            }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchResult.value = null
        _error.value = null
    }

    fun joinPublicRoom(roomId: String, onSuccess: (roomId: String) -> Unit) {
        viewModelScope.launch {
            _error.value = null
            when (val result = roomRepository.joinPublicRoom(roomId)) {
                is RoomResult.Success -> {
                    onSuccess(result.data.roomId)
                }

                is RoomResult.NotFound -> _error.value = JoinRoomError.RoomNotFound
                is RoomResult.RoomFull -> _error.value = JoinRoomError.RoomFull
                is RoomResult.AlreadyInRoom -> _error.value = JoinRoomError.AlreadyInRoom
                else -> _error.value = JoinRoomError.UnknownError
            }
        }
    }

    fun joinPrivateRoom(roomId:String, code: String, onSuccess: (roomId: String) -> Unit) {
        if (code.length != 6) {
            _error.value = JoinRoomError.InvalidCode
            return
        }
        viewModelScope.launch {
            _error.value = null
            when (val result = roomRepository.joinPrivateRoom(roomId = roomId, code = code)) {
                is RoomResult.Success -> {
                    onSuccess(result.data.roomId)
                }

                is RoomResult.NotFound -> _error.value = JoinRoomError.RoomNotFound
                is RoomResult.RoomFull -> _error.value = JoinRoomError.RoomFull
                is RoomResult.AlreadyInRoom -> _error.value = JoinRoomError.AlreadyInRoom
                else -> _error.value = JoinRoomError.UnknownError
            }
        }
    }
}

sealed class JoinRoomError {
    object LoadFailed : JoinRoomError()
    object InvalidCode : JoinRoomError()
    object RoomNotFound : JoinRoomError()
    object RoomFull : JoinRoomError()
    object AlreadyInRoom : JoinRoomError()
    object UnknownError : JoinRoomError()

    fun message(): String = when (this) {
        is LoadFailed -> "Failed to load rooms"
        is InvalidCode -> "Code must be 6 characters"
        is RoomNotFound -> "Room not found"
        is RoomFull -> "Room is full"
        is AlreadyInRoom -> "You are already in this room"
        is UnknownError -> "Something went wrong"
    }
}