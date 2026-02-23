package com.fran.dev.potjera.android.app.di

import android.R.attr.level
import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import com.fran.dev.potjera.android.app.auth.api.AuthApi
import com.fran.dev.potjera.android.app.auth.authenticators.TokenAuthenticator
import com.fran.dev.potjera.android.app.auth.interceptors.AuthInterceptor
import com.fran.dev.potjera.android.app.auth.repository.AuthRepository
import com.fran.dev.potjera.android.app.auth.repository.AuthRepositoryImpl
import com.fran.dev.potjera.android.app.room.api.RoomApi
import com.fran.dev.potjera.android.app.room.repository.RoomRepository
import com.fran.dev.potjera.android.app.room.repository.RoomRepositoryImpl
import com.fran.dev.potjera.android.app.user.api.UserApi
import com.fran.dev.potjera.android.app.user.repository.UserRepository
import com.fran.dev.potjera.android.app.user.repository.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Qualifier
import jakarta.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    const val BASE_URL = "http://10.0.2.2:8080/api/"

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("RETROFIT", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // ← shared client with logging
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        return retrofit.create(UserApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRoomApi(retrofit: Retrofit): RoomApi {
        return retrofit.create(RoomApi::class.java)
    }

    @Provides
    @Singleton
    @NoAuthRetrofit
    fun provideNoAuthRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSharedPref(app: Application): SharedPreferences {
        return app.getSharedPreferences("prefs", MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(api: AuthApi, prefs: SharedPreferences): AuthRepository {
        return AuthRepositoryImpl(api, prefs)
    }

    @Provides
    @Singleton
    fun provideUserRepository(api: UserApi): UserRepository {
        return UserRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideRoomRepository(api: RoomApi): RoomRepository {
        return RoomRepositoryImpl(api)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuthRetrofit
