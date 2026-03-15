package com.fran.dev.potjera.android.app.game.presentation

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.game.models.enums.GamePhase
import com.fran.dev.potjera.android.app.game.models.event.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.models.state.GameSessionState
import com.fran.dev.potjera.android.app.game.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GameSessionViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    private val repository: GameSessionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "GameSessionViewModel"
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    val myPlayerId: Long = prefs.getLong("user_id", 0L)

    // ── Session state ─────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(GameSessionState())
    val state: StateFlow<GameSessionState> = _state

    val gameSessionId: String
        get() = _state.value.gameSessionId

    // ── Derived identity flags ────────────────────────────────────────────────

    val isHunter: StateFlow<Boolean> = _state.map {
        it.gameSessionPlayers[myPlayerId]?.isHunter == true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isHost: StateFlow<Boolean> = _state.map {
        it.gameSessionPlayers.values.firstOrNull { p -> p.isHost }?.playerId == myPlayerId
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isCaptain: StateFlow<Boolean> = _state.map {
        it.gameSessionPlayers[myPlayerId]?.isCaptain == true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSpectator: StateFlow<Boolean> = _state.map {
        it.gameSessionPlayers[myPlayerId]?.isEliminated == true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val captainId: StateFlow<Long?> = _state.map { s ->
        s.gameSessionPlayers.values.firstOrNull { it.isCaptain }?.playerId
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val hunterId: StateFlow<Long> = _state.map { s ->
        s.gameSessionPlayers.values.firstOrNull { it.isHunter }?.playerId ?: 0L
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val players: StateFlow<List<GameSessionPlayer>> = _state.map {
        it.gameSessionPlayers.values.toList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Init ──────────────────────────────────────────────────────────────────

    fun init(gameSessionId: String) {
        Log.d(TAG, "init: $gameSessionId")
        _state.update { it.copy(gameSessionId = gameSessionId) }

        val token = prefs.getString("token", null) ?: return
        repository.connect(gameSessionId, token)

        viewModelScope.launch {
            repository.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    // ── Event handler — only phase transitions and player list updates ─────────

    private fun handleEvent(event: GameSessionSocketEvent) {
        when (event) {
            // Player list arrives with coin booster start
            is GameSessionSocketEvent.CoinBoosterStartedPlayerEvent -> {
                _state.update {
                    it.copy(
                        gameSessionPlayers = event.dto.playersInfo,
                        gamePhase = GamePhase.COIN_BOOSTER
                    )
                }
            }

            is GameSessionSocketEvent.CoinBoosterStartedHunterEvent -> {
                _state.update {
                    it.copy(
                        gameSessionPlayers = event.dto.playersInfo,
                        gamePhase = GamePhase.COIN_BOOSTER
                    )
                }
            }

            is GameSessionSocketEvent.BoardPhaseStartingEvent -> {
                _state.update { it.copy(gamePhase = GamePhase.BOARD) }
            }

            is GameSessionSocketEvent.PlayersAnsweringPhaseStartEvent -> {
                _state.update { it.copy(gamePhase = GamePhase.PLAYERS_ANSWERING) }
            }

            is GameSessionSocketEvent.HunterAnsweringPhaseStartEvent -> {
                _state.update { it.copy(gamePhase = GamePhase.HUNTER_ANSWERING) }
            }

            is GameSessionSocketEvent.GameFinishedEvent -> {
                _state.update { it.copy(gamePhase = GamePhase.FINISHED) }
            }

            // Player list updates mid-game
            is GameSessionSocketEvent.PlayerWonEvent -> {
                _state.update { it.copy(gameSessionPlayers = event.dto.playersListUpdated) }
            }

            is GameSessionSocketEvent.PlayerCaughtEvent -> {
                _state.update { it.copy(gameSessionPlayers = event.dto.playersListUpdated) }
            }

            is GameSessionSocketEvent.HunterAnsweringPhaseFinishedEvent -> {
                viewModelScope.launch {
                    delay(3000)
                    _state.update { it.copy(gamePhase = GamePhase.FINISHED) }
                }
            }


            else -> Unit
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}