package com.fran.dev.potjera.android.app.auth.authenticators;

import android.content.SharedPreferences
import com.fran.dev.potjera.android.app.auth.api.AuthApi
import com.fran.dev.potjera.android.app.auth.api.RefreshRequest
import com.fran.dev.potjera.android.app.di.NoAuthRetrofit
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import javax.inject.Inject
import androidx.core.content.edit

class TokenAuthenticator @Inject constructor(
    private val prefs: SharedPreferences,
    @NoAuthRetrofit private val retrofit: Retrofit
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        // 1️⃣ Prevent infinite retry loops
        if (responseCount(response) >= 2) return null

        // 2️⃣ ⬇️ PUT IT RIGHT HERE
        // Prevent trying to refresh if refresh endpoint itself returned 401
        if (response.request.url.encodedPath.contains("/auth/refresh")) {
            return null
        }

        // 3️⃣ Continue normal refresh flow
        val refreshToken = prefs.getString("refreshToken", null) ?: return null

        val refreshApi = retrofit.create(AuthApi::class.java)

        val refreshResponse = try {
            refreshApi.refreshBlocking(RefreshRequest(refreshToken)).execute()
        } catch (e: Exception) {
            return null
        }

        if (!refreshResponse.isSuccessful) return null

        val body = refreshResponse.body() ?: return null

        val newAccessToken = body.token

        prefs.edit {
            putString("token", newAccessToken)
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var res = response.priorResponse
        while (res != null) {
            result++
            res = res.priorResponse
        }
        return result
    }
}
