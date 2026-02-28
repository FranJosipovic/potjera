package com.fran.dev.potjera.android.app.room.repository

import android.util.Log
import com.fran.dev.potjera.android.app.room.api.AssignHunterRequest
import com.fran.dev.potjera.android.app.room.api.CreateRoomRequest
import com.fran.dev.potjera.android.app.room.api.CreateRoomResponse
import com.fran.dev.potjera.android.app.room.api.RoomApi
import com.fran.dev.potjera.android.app.room.api.RoomDetailsResponse
import retrofit2.HttpException

class RoomRepositoryImpl(
    private val api: RoomApi,
) : RoomRepository {

    companion object {
        private const val TAG = "RoomRepository"
    }

    override suspend fun createRoom(isPrivate: Boolean): RoomResult<CreateRoomResponse> {
        return try {
            Log.d(TAG, "createRoom: isPrivate=$isPrivate")
            val response = api.createRoom(request = CreateRoomRequest(isPrivate = isPrivate))
            Log.d(TAG, "createRoom: success roomId=${response.roomId}")
            RoomResult.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "createRoom: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }

    override suspend fun getRoomDetails(roomId: String): RoomResult<RoomDetailsResponse> {
        return try {
            Log.d(TAG, "getRoomDetails: roomId=$roomId")
            val response = api.getRoomDetails(roomId)
            Log.d(TAG, "getRoomDetails: success players=${response.players.size}")
            RoomResult.Success(response)
        } catch (e: HttpException) {
            Log.e(TAG, "getRoomDetails: HTTP ${e.code()} ${e.message()}")
            when (e.code()) {
                404 -> RoomResult.NotFound()
                else -> RoomResult.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRoomDetails: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }

    override suspend fun getPublicRooms(): RoomResult<List<RoomDetailsResponse>> {
        return try {
            Log.d(TAG, "getPublicRooms: fetching...")
            val response = api.getPublicRooms()
            Log.d(TAG, "getPublicRooms: success count=${response.size}")
            RoomResult.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "getPublicRooms: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }

    override suspend fun getRoomByCode(code: String): RoomResult<RoomDetailsResponse> {
        return try {
            Log.d(TAG, "getRoomByCode: code=$code")
            val response = api.getRoomByCode(code)
            Log.d(TAG, "getRoomByCode: success roomId=${response.id}")
            RoomResult.Success(response)
        } catch (e: HttpException) {
            Log.e(TAG, "getRoomByCode: HTTP ${e.code()} ${e.message()}")
            when (e.code()) {
                404 -> RoomResult.NotFound()
                else -> RoomResult.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRoomByCode: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }

    override suspend fun startGame(roomId: String): RoomResult<Unit> {
        return try {
            api.startGame(roomId)
            RoomResult.Success(Unit)
        } catch (e: HttpException) {
            when (e.code()) {
                403 -> RoomResult.Forbidden()
                404 -> RoomResult.NotFound()
                else -> RoomResult.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "startGame: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }

    override suspend fun joinPublicRoom(roomId: String): RoomResult<CreateRoomResponse> {
        return try {
            Log.d(TAG, "joinPublicRoom: roomId=$roomId")
            val response = api.joinPublicRoom(roomId)
            Log.d(TAG, "joinPublicRoom: success roomId=${response.roomId}")
            RoomResult.Success(response)
        } catch (e: HttpException) {
            Log.e(TAG, "joinPublicRoom: HTTP ${e.code()} ${e.message()}")
            when (e.code()) {
                404 -> RoomResult.NotFound()
                409 -> RoomResult.AlreadyInRoom()
                403 -> RoomResult.RoomFull()
                else -> RoomResult.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "joinPublicRoom: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }

    override suspend fun joinPrivateRoom(code: String): RoomResult<CreateRoomResponse> {
        return try {
            Log.d(TAG, "joinPrivateRoom: code=$code")
            val response = api.joinPrivateRoom(code)
            Log.d(TAG, "joinPrivateRoom: success roomId=${response.roomId}")
            RoomResult.Success(response)
        } catch (e: HttpException) {
            Log.e(TAG, "joinPrivateRoom: HTTP ${e.code()} ${e.message()}")
            when (e.code()) {
                404 -> RoomResult.NotFound()
                409 -> RoomResult.AlreadyInRoom()
                403 -> RoomResult.RoomFull()
                else -> RoomResult.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "joinPrivateRoom: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }

    override suspend fun leaveRoom(roomId: String): RoomResult<Unit> {
        return try {
            Log.d(TAG, "leaveRoom: roomId=$roomId")
            api.leave(roomId)
            RoomResult.Success(Unit)
        } catch (e: HttpException) {
            Log.e(TAG, "leaveRoom: HTTP ${e.code()} ${e.message()}")
            when (e.code()) {
                404 -> RoomResult.NotFound()
                400 -> RoomResult.UnknownError()
                else -> RoomResult.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "leaveRoom: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }

    override suspend fun assignHunter(roomId: String, hunterId: Long): RoomResult<Unit> {
        return try {
            Log.d(TAG, "assignHunter: roomId=$roomId hunterId=$hunterId")
            api.assignHunter(roomId, AssignHunterRequest(hunterId))
            RoomResult.Success(Unit)
        } catch (e: HttpException) {
            Log.e(TAG, "assignHunter: HTTP ${e.code()} ${e.message()}")
            when (e.code()) {
                403 -> RoomResult.Forbidden()
                404 -> RoomResult.NotFound()
                else -> RoomResult.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "assignHunter: ${e.message}", e)
            RoomResult.UnknownError()
        }
    }
}
