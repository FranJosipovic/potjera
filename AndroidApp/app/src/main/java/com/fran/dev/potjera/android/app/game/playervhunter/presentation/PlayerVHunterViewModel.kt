package com.fran.dev.potjera.android.app.game.playervhunter.presentation

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.fran.dev.potjera.android.app.game.services.GameSessionSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class PlayerVHunterViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    private val gameSessionSocketService: GameSessionSocketService
) : ViewModel() {

    val myPlayerId: Long = prefs.getLong("user_id", 0L)
}