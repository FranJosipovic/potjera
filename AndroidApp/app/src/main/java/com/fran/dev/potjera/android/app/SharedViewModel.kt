package com.fran.dev.potjera.android.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.domain.models.user.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUser(false)
    }

    fun setUser(user: User) {
        _user.value = user
    }

    fun loadUser(should: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true

            delay(1000)

            if(should){
                _user.value = User(
                    username = "Player_123",
                    imageUrl = null,
                    coins = 12_250,
                    rank = 47,
                    xp = 2450,
                    gamesPlayed = 38,
                    gamesWon = 24
                )

            }else{
                _user.value = null
            }
            // your real data fetch here
            _isLoading.value = false
        }
    }
}