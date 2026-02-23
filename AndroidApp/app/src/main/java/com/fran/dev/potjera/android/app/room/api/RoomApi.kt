package com.fran.dev.potjera.android.app.room.api

import com.fran.dev.potjera.android.app.domain.models.enums.RoomStatus
import retrofit2.http.Body
import retrofit2.http.POST
import java.time.LocalDateTime

interface RoomApi {

    @POST("rooms/create")
    suspend fun createRoom(
        @Body request: CreateRoomRequest
    ): CreateRoomResponse
}

data class CreateRoomRequest(
    val isPrivate: Boolean,
)

data class CreateRoomResponse(
    val roomId: String,
    val code: String?,        // null if public
    val status: RoomStatus,
    val maxPlayers: Int,
    val createdAt: LocalDateTime
)
