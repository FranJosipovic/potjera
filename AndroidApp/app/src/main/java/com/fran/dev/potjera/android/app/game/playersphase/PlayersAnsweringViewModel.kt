package com.fran.dev.potjera.android.app.game.playersphase

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.di.GameSessionRepositoryFactory
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.game.models.event.GameSessionEvent
import com.fran.dev.potjera.android.app.game.models.state.PlayersAnsweringPlayer
import com.fran.dev.potjera.android.app.game.repository.Difficulty
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
    private val repositoryFactory: GameSessionRepositoryFactory,
) : ViewModel() {

    companion object {
        private const val TAG = "PlayersAnsweringVM"
    }

    private lateinit var repository: GameSessionRepository

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

    private val _phaseFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val phaseFinished: SharedFlow<Unit> = _phaseFinished.asSharedFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var gameSessionId: String = ""
    private val phaseEmojis = listOf("🎮", "🧠", "⚡", "🎯")

    // ── Init ──────────────────────────────────────────────────────────────────

    fun init(gameSessionId: String, difficulty: Difficulty?) {
        Log.d(TAG, "init: gameSessionId=$gameSessionId difficulty=$difficulty")
        repository = repositoryFactory.get(difficulty)
        viewModelScope.launch {
            repository.events.collect { event -> handleEvent(event) }
        }
    }

    fun setContext(gameSessionId: String, allPlayers: Map<Long, GameSessionPlayer>) {
        this.gameSessionId = gameSessionId

        val eligible = allPlayers.values.filter { !it.isHunter && !it.isEliminated }
        _playerList.value = eligible.map {
            PlayersAnsweringPlayer(
                playerId = it.playerId,
                name     = it.playerName,
                emoji    = phaseEmojis[it.playerId.toInt() % phaseEmojis.size]
            )
        }
        Log.d(TAG, "setContext: players=${_playerList.value.map { it.name }}")
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun buzzIn() {
        Log.d(TAG, "buzzIn")
        viewModelScope.launch { repository.buzzIn() }
    }

    fun answerQuestion(answer: String) {
        Log.d(TAG, "answerQuestion: \"$answer\"")
        viewModelScope.launch { repository.sendPlayersAnsweringAnswer(answer) }
    }

    // ── Event handler ─────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionEvent) {
        when (event) {
            is GameSessionEvent.PlayersAnsweringPhaseStartEvent -> {
                val steps = _playerList.value.size
                Log.i(TAG, "PlayersAnsweringPhaseStart: question=\"${event.dto.question.question}\"" +
                        " questionNum=${event.dto.questionNum} totalSteps=$steps")
                _totalSteps.update { steps }
                _questionText.update { event.dto.question.question }
                _currentAnsweringPlayerId.value = null
                _correctAnswer.value = null
                _playerAnsweredCorrectly.value = null
            }

            is GameSessionEvent.PlayerBuzzedInEvent -> {
                val name = _playerList.value.firstOrNull { it.playerId == event.dto.playerId }?.name ?: event.dto.playerId
                Log.d(TAG, "PlayerBuzzedIn: $name (id=${event.dto.playerId})")
                _currentAnsweringPlayerId.value = event.dto.playerId
            }

            is GameSessionEvent.PlayersAnsweringCorrectEvent -> {
                val name = _playerList.value.firstOrNull { it.playerId == event.dto.playerId }?.name ?: event.dto.playerId
                Log.i(TAG, "PlayersAnsweringCorrect: $name answered correctly — totalSteps=${_totalSteps.value + 1}")
                _playerAnsweredCorrectly.update { true }
                _currentAnsweringPlayerId.update { null }
                _correctAnswer.update { event.dto.correctAnswer }
                _totalSteps.update { it + 1 }
            }

            is GameSessionEvent.PlayersAnsweringWrongEvent -> {
                val name = _playerList.value.firstOrNull { it.playerId == event.dto.playerId }?.name ?: event.dto.playerId
                Log.d(TAG, "PlayersAnsweringWrong: $name wrong — correct was \"${event.dto.correctAnswer}\"")
                _playerAnsweredCorrectly.update { false }
                _correctAnswer.update { event.dto.correctAnswer }
            }

            is GameSessionEvent.PlayersAnsweringNextQuestionEvent -> {
                Log.d(TAG, "PlayersAnsweringNextQuestion: \"${event.dto.question.question}\"" +
                        " (${event.dto.questionNum}/${event.dto.total})")
                _playerAnsweredCorrectly.value = null
                _currentAnsweringPlayerId.update { null }
                _correctAnswer.update { null }
                _questionText.update { event.dto.question.question }
            }

            is GameSessionEvent.PlayersAnsweringPhaseFinishedEvent -> {
                Log.i(TAG, "PlayersAnsweringPhaseFinished: totalSteps=${_totalSteps.value}")
                viewModelScope.launch { _phaseFinished.emit(Unit) }
            }

            else -> Unit
        }
    }
}