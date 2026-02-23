package com.fran.dev.potjera.android.app.room.repository

import com.fran.dev.potjera.android.app.room.api.CreateRoomRequest
import com.fran.dev.potjera.android.app.room.api.CreateRoomResponse
import com.fran.dev.potjera.android.app.room.api.RoomApi

class RoomRepositoryImpl(
    private val api: RoomApi,
) : RoomRepository {
    override suspend fun createRoom(isPrivate: Boolean): CreateRoomResult<CreateRoomResponse> {
        return try {
            val response = api.createRoom(
                request = CreateRoomRequest(
                    isPrivate = isPrivate
                )
            )
            CreateRoomResult.Success(response)
        } catch (e: Exception) {
            CreateRoomResult.UnknownError()
        }
    }
}
