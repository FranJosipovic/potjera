package com.fran.dev.potjera.android.app.auth.api

import retrofit2.Call
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): SignUpResponse

    @POST("auth/login")
    suspend fun signIn(
        @Body request: SignInRequest
    ): SignInResponse

    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): ResponseBody

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): RefreshResponse

    // ✅ ADD THIS (blocking version)
    @POST("auth/refresh")
    fun refreshBlocking(@Body request: RefreshRequest): Call<RefreshResponse>

    @POST("auth/check-status")
    suspend fun checkStatus(
        @Body request: CheckStatusRequest
    ): CheckStatusResponse
}

data class SignUpRequest(
    val username: String,
    val email: String,
    val password: String
)

data class SignInRequest(
    val email: String,
    val password: String
)

data class SignInResponse(
    val userId: Long,
    val token: String,
    val refreshToken: String,
    val username: String,
    val email: String
)

data class SignUpResponse(
    val userId: Long,
    val token: String,
    val refreshToken: String,
    val username: String,
    val email: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class RefreshResponse(
    val token: String,
)

data class CheckStatusRequest(
    val token: String,
    val refreshToken: String
)

data class CheckStatusResponse(
    val token: String,
    val refreshToken: String
)

data class LogoutRequest(
    val refreshToken: String
)