package com.fran.dev.potjera.android.app.auth.repository

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.fran.dev.potjera.android.app.auth.api.AuthApi
import com.fran.dev.potjera.android.app.auth.api.CheckStatusRequest
import com.fran.dev.potjera.android.app.auth.api.CheckStatusResponse
import com.fran.dev.potjera.android.app.auth.api.LogoutRequest
import com.fran.dev.potjera.android.app.auth.api.RefreshRequest
import com.fran.dev.potjera.android.app.auth.api.RefreshResponse
import com.fran.dev.potjera.android.app.auth.api.SignInRequest
import com.fran.dev.potjera.android.app.auth.api.SignInResponse
import com.fran.dev.potjera.android.app.auth.api.SignUpRequest
import com.fran.dev.potjera.android.app.auth.api.SignUpResponse
import retrofit2.HttpException

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val prefs: SharedPreferences
) : AuthRepository {

    companion object {
        const val TAG = "AuthRepositoryImpl"
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): AuthResult<SignInResponse> {
        return try {
            val response = api.signIn(
                request = SignInRequest(
                    email = email,
                    password = password
                )
            )
            prefs.edit {
                putString("token", response.token)
                    .putString("refreshToken", response.refreshToken)
            }
            AuthResult.Authorized(response)
        } catch (e: Exception) {
            AuthResult.UnknownError()
        }
    }

    override suspend fun signUp(
        username: String,
        email: String,
        password: String
    ): AuthResult<SignUpResponse> {
        return try {
            val response = api.signUp(
                request = SignUpRequest(
                    username = username,
                    password = password,
                    email = email
                )
            )
            prefs.edit {
                putString("token", response.token)
                    .putString("refreshToken", response.refreshToken)
            }
            AuthResult.Authorized(response)
        } catch (e: HttpException) {
            Log.e(TAG, "signUp: ${e.message}", e.cause)
            if (e.code() == 401) {
                AuthResult.Unauthorized()
            } else {
                AuthResult.UnknownError()
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUp: ${e.message}", e.cause)
            AuthResult.UnknownError()
        }
    }

    override suspend fun refreshToken(refreshToken: String): RefreshTokenResult<RefreshResponse> {
        return try {
            val response = api.refresh(RefreshRequest(refreshToken))
            prefs.edit {
                putString("token", response.token)
            }
            RefreshTokenResult.Success(response)
        } catch (e: Exception) {
            RefreshTokenResult.Error()
        }
    }

    override suspend fun checkStatus(
        token: String,
        refreshToken: String
    ): CheckStatusResult<CheckStatusResponse> {
        return try {
            val response = api.checkStatus(CheckStatusRequest(token, refreshToken))
            prefs.edit {
                putString("token", response.token)
                    .putString("refreshToken", response.refreshToken)
            }
            CheckStatusResult.Authorized(response)
        } catch (e: HttpException) {
            CheckStatusResult.Unauthorized()
        } catch (e: Exception) {
            CheckStatusResult.UnknownError()
        }
    }

    override suspend fun logout(refreshToken: String): LogoutResult {
        return try {

            Log.d(TAG, "logout: refreshToken: $refreshToken")

            api.logout(LogoutRequest(refreshToken))

            prefs.edit {
                clear()
            }

            LogoutResult.Success
        } catch (e: HttpException) {
            Log.e(TAG, "logout: ${e.message}, ${e.response()}", e.cause)
            LogoutResult.Error
        } catch (e: Exception) {
            Log.e(TAG, "logout: ${e.message}", e.cause)
            LogoutResult.Error
        }
    }
}