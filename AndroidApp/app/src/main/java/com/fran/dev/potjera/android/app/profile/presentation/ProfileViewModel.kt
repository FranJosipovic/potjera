package com.fran.dev.potjera.android.app.profile.presentation

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.auth.repository.AuthRepository
import com.fran.dev.potjera.android.app.auth.repository.LogoutResult
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val refreshToken = prefs.getString("refreshToken", null) ?: return@launch
            val response = authRepository.logout(refreshToken)

            when (response) {
                LogoutResult.Error -> {
                    Log.d(TAG, "logout: error")
                }

                LogoutResult.Success -> {
                    onSuccess()
                }
            }

        }
    }


}