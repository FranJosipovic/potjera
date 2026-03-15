package com.fran.dev.potjera.android.app.room.model

import com.fran.dev.potjera.android.app.domain.models.enums.RoomStatus


data class RoomDetailsResponse(
    val id: String,
    val code: String,
    val status: String,
    val maxPlayers: Int,
    val currentPlayers: Int,
    val createdAt: String,
    val players: List<RoomPlayerDTO>,
    val hunter: RoomPlayerDTO?
)


data class CreateRoomResponse(
    val roomId: String,
    val code: String?,        // null if public
    val status: RoomStatus,
    val maxPlayers: Int,
    val createdAt: String
)
