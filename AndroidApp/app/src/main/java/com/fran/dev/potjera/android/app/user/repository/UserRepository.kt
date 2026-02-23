package com.fran.dev.potjera.android.app.user.repository

import com.fran.dev.potjera.android.app.user.api.MeResponse

interface UserRepository {
    suspend fun me(): MeResult<MeResponse>
}

sealed class MeResult<T>(val data: T? = null) {
    class Success<T>(data: T) : MeResult<T>(data)
    class Error<T>(data: T? = null) : MeResult<T>(data)
}
