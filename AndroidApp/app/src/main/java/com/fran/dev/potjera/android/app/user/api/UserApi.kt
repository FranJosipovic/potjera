package com.fran.dev.potjera.android.app.user.api

import com.fran.dev.potjera.android.app.auth.api.SignUpRequest
import com.fran.dev.potjera.android.app.auth.api.SignUpResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.time.LocalDateTime

interface UserApi {
    @GET("users/me")
    suspend fun me(): MeResponse
}

data class MeResponse(
    val id: Long,
    val username: String,
    val email: String,
    val createdAt: String
)