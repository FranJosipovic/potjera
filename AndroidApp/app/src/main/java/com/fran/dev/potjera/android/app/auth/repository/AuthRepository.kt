package com.fran.dev.potjera.android.app.auth.repository

import com.fran.dev.potjera.android.app.auth.api.CheckStatusResponse
import com.fran.dev.potjera.android.app.auth.api.LogoutRequest
import com.fran.dev.potjera.android.app.auth.api.RefreshResponse
import com.fran.dev.potjera.android.app.auth.api.SignInResponse
import com.fran.dev.potjera.android.app.auth.api.SignUpResponse

interface AuthRepository {
    suspend fun signIn(email: String, password: String): AuthResult<SignInResponse>
    suspend fun signUp(username: String, email: String, password: String): AuthResult<SignUpResponse>
    suspend fun refreshToken(refreshToken: String): RefreshTokenResult<RefreshResponse>
    suspend fun checkStatus(token: String, refreshToken: String): CheckStatusResult<CheckStatusResponse>
    suspend fun logout(refreshToken: String): LogoutResult
}

sealed class AuthResult<T>(val data: T? = null) {
    class Authorized<T>(data: T? = null): AuthResult<T>(data)
    class Unauthorized<T>: AuthResult<T>()
    class UnknownError<T>: AuthResult<T>()
}

sealed class RefreshTokenResult<T>(val data: T? = null){
    class Success<T>(data: T? = null): RefreshTokenResult<T>(data)
    class Error<T>(data: T? = null): RefreshTokenResult<T>()
}

sealed class CheckStatusResult<T>(val data: T? = null){
    class Authorized<T>(data: T? = null): CheckStatusResult<T>(data)
    class Unauthorized<T>(data: T? = null): CheckStatusResult<T>(data)
    class UnknownError<T>: CheckStatusResult<T>()
}

sealed class LogoutResult(){
    object Success: LogoutResult()
    object Error: LogoutResult()
}