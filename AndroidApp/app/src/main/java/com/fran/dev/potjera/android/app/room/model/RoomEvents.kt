package com.fran.dev.potjera.android.app.room.model

sealed class LobbyEvent {
    data class StartGame(val gameSessionId: String) : LobbyEvent()
    object RoomClosed : LobbyEvent()
}

sealed class RoomSocketEvent {
    data class PlayerJoined(val player: PlayerJoinedDto) : RoomSocketEvent()
    data class GameStarting(val payload: GameStartingDto) : RoomSocketEvent()
    data class HunterChanged(val payload: HunterChangedDto) : RoomSocketEvent()
    data class CaptainChanged(val payload: CaptainChangedDto) : RoomSocketEvent()
    data class PlayerLeftRoom(val payLoad: PlayerLeftRoomDto) : RoomSocketEvent()
    data class RoomClosed(val payload: RoomClosedDto): RoomSocketEvent()
}