package com.fran.dev.potjera.android.app.room.repository

import com.fran.dev.potjera.android.app.room.model.CreateRoomResponse
import com.fran.dev.potjera.android.app.room.model.RoomDetailsResponse

interface RoomRepository {
    suspend fun createRoom(isPrivate: Boolean): RoomResult<CreateRoomResponse>
    suspend fun joinPublicRoom(roomId: String): RoomResult<CreateRoomResponse>
    suspend fun joinPrivateRoom(roomId:String, code: String): RoomResult<CreateRoomResponse>
    suspend fun getRoomDetails(roomId: String): RoomResult<RoomDetailsResponse>
    suspend fun getPublicRooms(): RoomResult<List<RoomDetailsResponse>>
    suspend fun getRoomByCode(code: String): RoomResult<RoomDetailsResponse>
    suspend fun startGame(roomId: String): RoomResult<Unit>
    suspend fun assignHunter(roomId: String, hunterId: Long): RoomResult<Unit>
    suspend fun assignCaptain(roomId: String, captainId: Long): RoomResult<Unit>
    suspend fun leaveRoom(roomId: String): RoomResult<Unit>
    suspend fun searchByName(name: String): RoomResult<RoomDetailsResponse>
}

sealed class RoomResult<out T> {
    data class Success<T>(val data: T) : RoomResult<T>()
    class NotFound : RoomResult<Nothing>()
    class AlreadyInRoom : RoomResult<Nothing>()
    class RoomFull : RoomResult<Nothing>()
    class UnknownError : RoomResult<Nothing>()
    class Forbidden : RoomResult<Nothing>()
}
