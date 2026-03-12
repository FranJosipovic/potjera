package com.fran.dev.potjera.android.app.room.api

import com.fran.dev.potjera.android.app.domain.models.enums.RoomStatus
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class RoomPlayerDTO(
    val playerId: Long,
    val username: String,
    val rank: Int,
    val isHost: Boolean,
    val isReady: Boolean,
    val isHunter: Boolean,
    val isCaptain: Boolean
)

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

data class JoinPrivateRoomRequest(
    val code: String
)

interface RoomApi {
    @POST("rooms/create")
    suspend fun createRoom(@Body request: CreateRoomRequest): CreateRoomResponse

    @POST("rooms/join/public/{roomId}")     // join public room by id
    suspend fun joinPublicRoom(@Path("roomId") roomId: String): CreateRoomResponse

    @POST("rooms/join/private/{roomId}")      // join private room by code
    suspend fun joinPrivateRoom(
        @Path("roomId") roomId: String,
        @Body request: JoinPrivateRoomRequest
    ): CreateRoomResponse

    @GET("rooms/{roomId}")
    suspend fun getRoomDetails(@Path("roomId") roomId: String): RoomDetailsResponse

    @GET("rooms")
    suspend fun getRooms(): List<RoomDetailsResponse>

    @GET("rooms/code/{code}")
    suspend fun getRoomByCode(@Path("code") code: String): RoomDetailsResponse

    @POST("rooms/{roomId}/start")
    suspend fun startGame(@Path("roomId") roomId: String)

    @POST("rooms/{roomId}/assign-hunter")
    suspend fun assignHunter(
        @Path("roomId") roomId: String,
        @Body request: AssignHunterRequest
    )

    @POST("rooms/{roomId}/assign-captain")
    suspend fun assignCaptain(
        @Path("roomId") roomId: String,
        @Body request: AssignCaptainRequest
    )

    @POST("rooms/{roomId}/leave")
    suspend fun leave(@Path("roomId") roomId: String)

    @GET("rooms/search/{name}")
    suspend fun searchByName(@Path("name") name: String): RoomDetailsResponse
}

data class AssignHunterRequest(val hunterId: Long)
data class AssignCaptainRequest(val captainId: Long)
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
