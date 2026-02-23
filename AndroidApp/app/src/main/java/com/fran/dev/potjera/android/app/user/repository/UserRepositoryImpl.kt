package com.fran.dev.potjera.android.app.user.repository

import android.util.Log
import com.fran.dev.potjera.android.app.user.api.MeResponse
import com.fran.dev.potjera.android.app.user.api.UserApi
import jakarta.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val api: UserApi
) : UserRepository {

    companion object {
        const val TAG = "UserRepository"
    }

    override suspend fun me(): MeResult<MeResponse> {
        return try {
            val response = api.me()
            MeResult.Success(response)
        } catch (e: Exception) {
            Log.e(TAG, "me: ${e.message}", e.cause)
            MeResult.Error()
        }
    }
}