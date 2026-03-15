package com.fran.dev.potjera.android.app.room.api

import com.fran.dev.potjera.android.app.room.model.AssignCaptainRequest
import com.fran.dev.potjera.android.app.room.model.AssignHunterRequest
import com.fran.dev.potjera.android.app.room.model.CreateRoomRequest
import com.fran.dev.potjera.android.app.room.model.CreateRoomResponse
import com.fran.dev.potjera.android.app.room.model.JoinPrivateRoomRequest
import com.fran.dev.potjera.android.app.room.model.RoomDetailsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


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
