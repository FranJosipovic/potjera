package com.fran.dev.potjera.android.app.room.presentation.lobby

import androidx.lifecycle.ViewModel
import com.fran.dev.potjera.android.app.room.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RoomLobbyViewModel @Inject constructor(
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _players = MutableStateFlow<List<LobbyPlayer>>(emptyList())
    val players: StateFlow<List<LobbyPlayer>> = _players.asStateFlow()

    private val _hunter = MutableStateFlow<LobbyHunter?>(null)
    val hunter: StateFlow<LobbyHunter?> = _hunter.asStateFlow()
}