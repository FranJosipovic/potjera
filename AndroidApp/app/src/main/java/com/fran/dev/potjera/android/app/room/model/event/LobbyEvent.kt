package com.fran.dev.potjera.android.app.room.model.event

sealed class LobbyEvent {
    data class StartGame(val gameSessionId: String) : LobbyEvent()
    object RoomClosed : LobbyEvent()
}
