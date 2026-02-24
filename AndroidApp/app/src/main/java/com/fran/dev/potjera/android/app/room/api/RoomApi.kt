package com.fran.dev.potjera.android.app.room.api

import com.fran.dev.potjera.android.app.domain.models.enums.RoomStatus
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.LocalDateTime

data class RoomPlayerDTO(
    val id: String,
    val playerId: Long,
    val username: String,
    val rank: Int,
    val isHost: Boolean,
    val isReady: Boolean,
    val isHunter: Boolean
)

data class RoomDetailsResponse(
    val id: String,
    val code: String?,
    val status: String,
    val maxPlayers: Int,
    val currentPlayers: Int,
    val createdAt: String,
    val players: List<RoomPlayerDTO>,
    val hunter: RoomPlayerDTO?
)

interface RoomApi {
    @POST("rooms/create")
    suspend fun createRoom(@Body request: CreateRoomRequest): CreateRoomResponse

    @POST("rooms/join/public/{roomId}")     // join public room by id
    suspend fun joinPublicRoom(@Path("roomId") roomId: String): CreateRoomResponse

    @POST("rooms/join/private/{code}")      // join private room by code
    suspend fun joinPrivateRoom(@Path("code") code: String): CreateRoomResponse

    @GET("rooms/{roomId}")
    suspend fun getRoomDetails(@Path("roomId") roomId: String): RoomDetailsResponse

    @GET("rooms/public")
    suspend fun getPublicRooms(): List<RoomDetailsResponse>

    @GET("rooms/code/{code}")
    suspend fun getRoomByCode(@Path("code") code: String): RoomDetailsResponse
}

data class CreateRoomRequest(
    val isPrivate: Boolean,
)

data class CreateRoomResponse(
    val roomId: String,
    val code: String?,        // null if public
    val status: RoomStatus,
    val maxPlayers: Int,
    val createdAt: String
)
