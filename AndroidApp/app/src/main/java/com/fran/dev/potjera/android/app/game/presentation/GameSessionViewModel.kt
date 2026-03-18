package com.fran.dev.potjera.android.app.game.presentation

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.di.GameSessionRepositoryFactory
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.game.models.enums.GamePhase
import com.fran.dev.potjera.android.app.game.models.event.GameSessionEvent
import com.fran.dev.potjera.android.app.game.models.state.GameSessionState
import com.fran.dev.potjera.android.app.game.repository.Difficulty
import com.fran.dev.potjera.android.app.game.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GameSessionViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    private val repositoryFactory: GameSessionRepositoryFactory,
) : ViewModel() {

    companion object {
        private const val TAG = "GameSessionViewModel"
    }

    private lateinit var repository: GameSessionRepository

    // ── Identity ──────────────────────────────────────────────────────────────

    val myPlayerId: Long = prefs.getLong("user_id", 0L)

    // ── Session state ─────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(GameSessionState())
    val state: StateFlow<GameSessionState> = _state.asStateFlow()

    val gameSessionId: String
        get() = _state.value.gameSessionId

    // ── Derived flags ─────────────────────────────────────────────────────────

    val isHunter: StateFlow<Boolean> = _state
        .map { it.gameSessionPlayers[myPlayerId]?.isHunter == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isHost: StateFlow<Boolean> = _state
        .map { it.gameSessionPlayers.values.firstOrNull { p -> p.isHost }?.playerId == myPlayerId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isCaptain: StateFlow<Boolean> = _state
        .map { it.gameSessionPlayers[myPlayerId]?.isCaptain == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isSpectator: StateFlow<Boolean> = _state
        .map { it.gameSessionPlayers[myPlayerId]?.isEliminated == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val captainId: StateFlow<Long?> = _state
        .map { s -> s.gameSessionPlayers.values.firstOrNull { it.isCaptain }?.playerId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val hunterId: StateFlow<Long> = _state
        .map { s -> s.gameSessionPlayers.values.firstOrNull { it.isHunter }?.playerId ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val players: StateFlow<List<GameSessionPlayer>> = _state
        .map { it.gameSessionPlayers.values.toList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Init ──────────────────────────────────────────────────────────────────

    fun init(gameSessionId: String, difficulty: Difficulty?) {
        Log.i(TAG, "init: gameSessionId=$gameSessionId difficulty=$difficulty")
        _state.update { it.copy(gameSessionId = gameSessionId) }

        repository = repositoryFactory.get(difficulty)
        Log.d(TAG, "init: using ${repository::class.simpleName}")


        collectEvents()
        if (difficulty != null) {
            viewModelScope.launch {
                Log.d(TAG, "init: connecting singleplayer with difficulty=$difficulty")
                repository.connect(difficulty)
            }
        } else {
            val token = prefs.getString("token", null)
            if (token == null) {
                Log.e(TAG, "init: no auth token found — cannot connect to multiplayer session")
                return
            }
            Log.d(TAG, "init: connecting multiplayer session=$gameSessionId")
            repository.connect(gameSessionId, token)
        }
    }

    private fun collectEvents() {
        viewModelScope.launch {
            Log.d(TAG, "collectEvents: started collecting")
            repository.events.collect { event -> handleEvent(event) }
        }
    }

    // ── Event handler ─────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionEvent) {
        Log.d(TAG, "handleEvent: ${event::class.simpleName}")
        when (event) {

            is GameSessionEvent.CoinBoosterStartedPlayerEvent -> {
                Log.i(
                    TAG, "handleEvent: coin booster started as PLAYER" +
                            " — players=${event.dto.playersInfo.values.map { it.playerName }}"
                )
                _state.update {
                    it.copy(
                        gameSessionPlayers = event.dto.playersInfo,
                        gamePhase = GamePhase.COIN_BOOSTER
                    )
                }
            }

            is GameSessionEvent.CoinBoosterStartedHunterEvent -> {
                Log.i(
                    TAG, "handleEvent: coin booster started as HUNTER" +
                            " — players=${event.dto.playersInfo.values.map { it.playerName }}"
                )
                _state.update {
                    it.copy(
                        gameSessionPlayers = event.dto.playersInfo,
                        gamePhase = GamePhase.COIN_BOOSTER
                    )
                }
            }

            is GameSessionEvent.BoardPhaseStartingEvent -> {
                Log.i(
                    TAG,
                    "handleEvent: board phase starting — currentPlayer=${event.dto.currentPlayerId}"
                )
                _state.update { it.copy(gamePhase = GamePhase.BOARD) }
            }

            is GameSessionEvent.PlayerWonEvent -> {
                val name =
                    event.dto.playersListUpdated[event.dto.playerWonId]?.playerName ?: "unknown"
                Log.i(
                    TAG,
                    "handleEvent: player won — id=${event.dto.playerWonId} name=$name money=\$${event.dto.moneyWon}"
                )
                _state.update { it.copy(gameSessionPlayers = event.dto.playersListUpdated) }
            }

            is GameSessionEvent.PlayerCaughtEvent -> {
                val name =
                    event.dto.playersListUpdated[event.dto.playerCaughtId]?.playerName ?: "unknown"
                Log.i(TAG, "handleEvent: player caught — id=${event.dto.playerCaughtId} name=$name")
                _state.update { it.copy(gameSessionPlayers = event.dto.playersListUpdated) }
            }

            is GameSessionEvent.BoardPhaseFinishedEvent -> {
                val survivors = event.dto.players.values.filter { !it.isHunter && !it.isEliminated }
                Log.i(
                    TAG,
                    "handleEvent: board phase finished — survivors=${survivors.map { it.playerName }}"
                )
                _state.update {
                    it.copy(
                        gameSessionPlayers = event.dto.players,
                        gamePhase = GamePhase.BOARD_SUMMARY
                    )
                }
            }

            is GameSessionEvent.PlayersAnsweringPhaseStartEvent -> {
                Log.i(
                    TAG,
                    "handleEvent: players answering phase started — question=\"${event.dto.question.question}\""
                )
                _state.update { it.copy(gamePhase = GamePhase.PLAYERS_ANSWERING) }
            }

            is GameSessionEvent.HunterAnsweringPhaseStartEvent -> {
                Log.i(
                    TAG, "handleEvent: hunter answering phase started" +
                            " — totalSteps=${event.dto.hunterAnsweringState.totalStepsToReach}"
                )
                _state.update { it.copy(gamePhase = GamePhase.HUNTER_ANSWERING) }
            }

            is GameSessionEvent.HunterAnsweringPhaseFinishedEvent -> {
                Log.i(
                    TAG,
                    "handleEvent: hunter answering phase finished — hunterWon=${event.dto.hunterWon}"
                )
                viewModelScope.launch {
                    delay(3_000)
                    Log.d(TAG, "handleEvent: transitioning to FINISHED after delay")
                    _state.update { it.copy(gamePhase = GamePhase.FINISHED) }
                }
            }

            is GameSessionEvent.GameFinishedEvent -> {
                Log.i(TAG, "handleEvent: game finished — ${event.results.size} results received")
                _state.update { it.copy(gamePhase = GamePhase.FINISHED) }
            }

            is GameSessionEvent.PlayersAnsweringPhaseFinishedEvent -> {
                Log.i(
                    TAG,
                    "PlayersAnsweringPhaseFinished: totalSteps=${event.dto.correctAnswers + event.dto.playerIds.size}}"
                )
            }

            else -> Unit
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: disconnecting repository")
        repository.disconnect()
    }
}