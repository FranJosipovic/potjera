package com.fran.dev.potjera.android.app.room.repository

import com.fran.dev.potjera.android.app.room.api.CreateRoomResponse

interface RoomRepository {
    suspend fun createRoom(isPrivate: Boolean): CreateRoomResult<CreateRoomResponse>
}

sealed class CreateRoomResult<T>(val data: T? = null) {
    class Success<T>(data: T? = null) : CreateRoomResult<T>(data)
    class UnknownError<T> : CreateRoomResult<T>()
}