package com.fran.dev.potjera.android.app.room.model


data class RoomPlayerDTO(
    val playerId: Long,
    val username: String,
    val rank: Int,
    val isHost: Boolean,
    val isReady: Boolean,
    val isHunter: Boolean,
    val isCaptain: Boolean
)

data class RoomEventDto(
    val type: String,
    val payload: Any
)

data class PlayerJoinedDto(
    val playerId: Long,
    val username: String,
    val isHunter: Boolean,
    val isReady: Boolean,
    val isCaptain: Boolean,
    val rank: Int
)

data class GameStartingDto(
    val gameSessionId: String,
    val message: String
)

data class HunterChangedDto(
    val playerId: Long
)

data class CaptainChangedDto(
    val playerId: Long
)

data class PlayerLeftRoomDto(
    val playerId: Long,
    val newHunterId: Long?
)

data class RoomClosedDto(
    val reason: String
)
