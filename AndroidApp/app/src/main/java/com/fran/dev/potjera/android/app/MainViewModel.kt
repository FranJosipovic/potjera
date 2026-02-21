package com.fran.dev.potjera.android.app

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.auth.repository.AuthRepository
import com.fran.dev.potjera.android.app.auth.repository.CheckStatusResult
import com.fran.dev.potjera.android.app.domain.models.user.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: SharedPreferences
) : ViewModel() {
    companion object {
        const val TAG = "MainViewModel"
    }

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _isLoading.value = true

            val token = prefs.getString("token", null)
            val refreshToken = prefs.getString("refreshToken", null)

            Log.d(TAG, "init: token: $token, refreshToken: $refreshToken")

            if (token != null && refreshToken != null) {
                val result = authRepository.checkStatus(token, refreshToken)
                when (result) {
                    is CheckStatusResult.Authorized<*> -> {
                        Log.d(TAG, "init: user is authorized")
                        //TODO: Load User Info
                        _user.value = User(
                            username = "Player_123",
                            imageUrl = null,
                            coins = 12_250,
                            rank = 47,
                            xp = 2450,
                            gamesPlayed = 38,
                            gamesWon = 24
                        )
                    }

                    is CheckStatusResult.Unauthorized<*> -> {
                        Log.d(TAG, "init: User is not authorized")
                        _user.value = null
                    }

                    is CheckStatusResult.UnknownError<*> -> {
                        Log.d(TAG, "init: Unknown error while checking user status")
                        _user.value = null
                    }
                }
            } else {
                Log.d(TAG, "User not authenticated")
                _user.value = null
            }

            _isLoading.value = false
        }
    }

    fun setUser(user: User?) {
        _user.value = user
    }
}