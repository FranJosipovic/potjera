package com.fran.dev.potjera.android.app.game.playersphase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.game.models.event.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.models.state.PlayersAnsweringPlayer
import com.fran.dev.potjera.android.app.game.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns: buzz-in state, player list for this phase, question text,
 * answer feedback, step progress.
 */
@HiltViewModel
class PlayersAnsweringViewModel @Inject constructor(
    private val repository: GameSessionRepository
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _playerList = MutableStateFlow<List<PlayersAnsweringPlayer>>(emptyList())
    val playerList: StateFlow<List<PlayersAnsweringPlayer>> = _playerList.asStateFlow()

    private val _currentAnsweringPlayerId = MutableStateFlow<Long?>(null)
    val currentAnsweringPlayerId: StateFlow<Long?> = _currentAnsweringPlayerId.asStateFlow()

    private val _questionText = MutableStateFlow<String?>(null)
    val questionText: StateFlow<String?> = _questionText.asStateFlow()

    private val _correctAnswer = MutableStateFlow<String?>(null)
    val correctAnswer: StateFlow<String?> = _correctAnswer.asStateFlow()

    private val _playerAnsweredCorrectly = MutableStateFlow<Boolean?>(null)
    val playerAnsweredCorrectly: StateFlow<Boolean?> = _playerAnsweredCorrectly.asStateFlow()

    private val _totalSteps = MutableStateFlow(0)
    val totalSteps: StateFlow<Int> = _totalSteps.asStateFlow()

    // One-shot event: phase finished
    private val _phaseFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val phaseFinished: SharedFlow<Unit> = _phaseFinished.asSharedFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var gameSessionId: String = ""
    private val phaseEmojis = listOf("🎮", "🧠", "⚡", "🎯")

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repository.events.collect { event -> handleEvent(event) }
        }
    }

    fun setContext(gameSessionId: String, allPlayers: Map<Long, GameSessionPlayer>) {
        this.gameSessionId = gameSessionId

        // Build the player list from current session players (non-hunter, non-eliminated)
        _playerList.value = allPlayers
            .filter { !it.value.isHunter && !it.value.isEliminated }
            .map {
                PlayersAnsweringPlayer(
                    playerId = it.value.playerId,
                    name = it.value.playerName,
                    emoji = phaseEmojis[it.value.playerId.toInt() % phaseEmojis.size]
                )
            }
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun buzzIn() {
        repository.sendBuzzIn(gameSessionId)
    }

    fun answerQuestion(answer: String) {
        repository.sendPlayersAnsweringAnswer(gameSessionId, answer)
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionSocketEvent) {
        when (event) {
            is GameSessionSocketEvent.PlayersAnsweringPhaseStartEvent -> {
                _totalSteps.update { _playerList.value.size }
                _questionText.update { event.dto.question.question }
                _currentAnsweringPlayerId.value = null
                _correctAnswer.value = null
                _playerAnsweredCorrectly.value = null
            }

            is GameSessionSocketEvent.PlayerBuzzedInEvent -> {
                _currentAnsweringPlayerId.value = event.dto.playerId
            }

            is GameSessionSocketEvent.PlayersAnsweringCorrectEvent -> {
                _playerAnsweredCorrectly.update { true }
                _currentAnsweringPlayerId.update { null }
                _correctAnswer.update { event.dto.correctAnswer }
                _totalSteps.update { it + 1 }
            }

            is GameSessionSocketEvent.PlayersAnsweringWrongEvent -> {
                _playerAnsweredCorrectly.update { false }
                _correctAnswer.update { event.dto.correctAnswer }
            }

            is GameSessionSocketEvent.PlayersAnsweringNextQuestionEvent -> {
                _playerAnsweredCorrectly.value = null
                _currentAnsweringPlayerId.update { null }
                _correctAnswer.update { null }
                _questionText.update { event.dto.question.question }
            }

            is GameSessionSocketEvent.PlayersAnsweringPhaseFinishedEvent -> {
                viewModelScope.launch { _phaseFinished.emit(Unit) }
            }

            else -> Unit
        }
    }
}