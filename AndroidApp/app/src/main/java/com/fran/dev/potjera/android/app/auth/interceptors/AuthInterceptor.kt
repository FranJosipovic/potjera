package com.fran.dev.potjera.android.app.auth.interceptors

import android.content.SharedPreferences
import jakarta.inject.Inject
import jakarta.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : Interceptor {

    private val ignoredEndpoints = listOf(
        "/auth",
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val shouldIgnore = ignoredEndpoints.any { path.startsWith(it) }
        if (shouldIgnore) return chain.proceed(originalRequest)

        val token = sharedPreferences.getString("token", null)
        if (token.isNullOrBlank()) return chain.proceed(originalRequest)

        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}