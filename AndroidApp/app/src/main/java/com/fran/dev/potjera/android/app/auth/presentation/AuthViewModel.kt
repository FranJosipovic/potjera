package com.fran.dev.potjera.android.app.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.auth.repository.AuthRepository
import com.fran.dev.potjera.android.app.domain.models.user.User
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        const val TAG = "AuthViewModel"
    }

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()


    fun signUp(username: String, email: String, password: String, onSuccess: (user: User) -> Unit) {
        viewModelScope.launch {
            try {

                Log.d(TAG, "signUp: username: $username, email: $email, password: $password")

                _loading.value = true
                val response = authRepository.signUp(
                    username = username,
                    email = email,
                    password = password
                )

                Log.d(TAG, "signUp: response: $response")

                if (response.data != null) {
                    val user = User(
                        username = response.data.username,
                        imageUrl = null,
                        coins = 0,
                        rank = 0,
                        xp = 0,
                        gamesPlayed = 0,
                        gamesWon = 0
                    )
                    onSuccess(user)
                }

            } catch (e: Exception) {
                Log.d(TAG, "signUp: error - $e")
            } finally {
                _loading.value = false
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: (user: User) -> Unit) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val response = authRepository.signIn(
                    email = email,
                    password = password
                )

                if (response.data != null) {
                    val user = User(
                        username = response.data.username,
                        imageUrl = null,
                        coins = 0,
                        rank = 0,
                        xp = 0,
                        gamesPlayed = 0,
                        gamesWon = 0
                    )
                    onSuccess(user)
                }
            } catch (e: Exception) {

            } finally {
                _loading.value = false
            }
        }
    }
}