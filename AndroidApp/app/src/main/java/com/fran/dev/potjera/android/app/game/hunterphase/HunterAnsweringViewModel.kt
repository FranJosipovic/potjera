package com.fran.dev.potjera.android.app.game.hunterphase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.models.SuggestionItem
import com.fran.dev.potjera.android.app.game.models.event.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.models.state.HunterAnsweringPhaseState
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
 * Owns: hunter answering phase state, suggestions list, timer endpoint,
 * and captain/player counter-answer flow.
 */
@HiltViewModel
class HunterAnsweringViewModel @Inject constructor(
    private val repository: GameSessionRepository
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _phaseState = MutableStateFlow(HunterAnsweringPhaseState())
    val phaseState: StateFlow<HunterAnsweringPhaseState> = _phaseState.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SuggestionItem>>(emptyList())
    val suggestions: StateFlow<List<SuggestionItem>> = _suggestions.asStateFlow()

    // One-shot event: phase finished
    private val _phaseFinished = MutableSharedFlow<HunterAnsweringResult>(extraBufferCapacity = 1)
    val phaseFinished: SharedFlow<HunterAnsweringResult> = _phaseFinished.asSharedFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var gameSessionId: String = ""
    private var playerList: List<PlayersAnsweringPlayer> = emptyList()

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repository.events.collect { event -> handleEvent(event) }
        }
    }

    fun setContext(
        gameSessionId: String,
        playerList: List<PlayersAnsweringPlayer>
    ) {
        this.gameSessionId = gameSessionId
        this.playerList = playerList
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun sendHunterAnswer(answer: String) {
        repository.sendHunterAnsweringAnswer(gameSessionId, answer)
    }

    fun sendPlayerCounterAnswer(answer: String) {
        repository.sendPlayerCounterAnswer(gameSessionId, answer)
    }

    fun sendSuggestion(suggestion: String) {
        repository.sendSuggestion(gameSessionId, suggestion)
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionSocketEvent) {
        when (event) {
            is GameSessionSocketEvent.HunterAnsweringPhaseStartEvent -> {
                val dto = event.dto
                _phaseState.update {
                    HunterAnsweringPhaseState(
                        playersSteps = dto.hunterAnsweringState.totalStepsToReach,
                        hunterCorrectAnswers = 0,
                        question = dto.question.question ?: "",
                        correctAnswer = null,
                        hunterIsAnswering = true,
                        playersAreAnswering = false,
                        players = playerList,
                        endTimestamp = dto.endTimestamp
                    )
                }
                _suggestions.value = emptyList()
            }

            is GameSessionSocketEvent.HunterAnsweredCorrectEvent -> {
                _phaseState.update {
                    it.copy(
                        hunterCorrectAnswers = it.hunterCorrectAnswers + 1,
                        correctAnswer = event.dto.correctAnswer,
                        hunterAnsweredCorrectly = true
                    )
                }
            }

            is GameSessionSocketEvent.HunterAnsweredWrongEvent -> {
                _phaseState.update {
                    it.copy(
                        hunterIsAnswering = false,
                        playersAreAnswering = true,
                        hunterAnsweredCorrectly = false
                    )
                }
            }

            is GameSessionSocketEvent.HunterAnsweringNextQuestionEvent -> {
                _phaseState.update {
                    it.copy(
                        question = event.dto.question,
                        correctAnswer = null,
                        hunterAnsweredCorrectly = null,
                        playersAnsweredCorrectly = null,
                        hunterIsAnswering = true,
                        playersAreAnswering = false
                    )
                }
                _suggestions.value = emptyList()
            }

            is GameSessionSocketEvent.PlayerCounterAnswerCorrectEvent -> {
                _phaseState.update {
                    val newPlayersSteps = if (it.hunterCorrectAnswers == 0) {
                        it.playersSteps + 1
                    } else {
                        it.playersSteps
                    }
                    val newHunterCorrect = if (it.hunterCorrectAnswers == 0) {
                        0
                    } else {
                        it.hunterCorrectAnswers - 1
                    }
                    it.copy(
                        correctAnswer = event.dto.correctAnswer,
                        playersAnsweredCorrectly = true,
                        hunterAnsweredCorrectly = false,
                        playersSteps = newPlayersSteps,
                        hunterCorrectAnswers = newHunterCorrect
                    )
                }
            }

            is GameSessionSocketEvent.PlayerCounterAnswerWrongEvent -> {
                _phaseState.update {
                    it.copy(
                        correctAnswer = event.dto.correctAnswer,
                        playersAnsweredCorrectly = false,
                        hunterAnsweredCorrectly = false
                    )
                }
            }

            is GameSessionSocketEvent.HunterAnsweringPhaseFinishedEvent -> {
                viewModelScope.launch {
                    _phaseState.update { it.copy(hunterWon = event.dto.hunterWon) }
                    viewModelScope.launch {
                        _phaseFinished.emit(
                            HunterAnsweringResult(hunterWon = event.dto.hunterWon)
                        )
                    }
                }
            }

            is GameSessionSocketEvent.HunterAnsweringSuggestionEvent -> {
                _suggestions.update {
                    it + SuggestionItem(
                        playerId = event.dto.sentBy,
                        username = event.dto.username,
                        suggestion = event.dto.suggestion
                    )
                }
            }

            is GameSessionSocketEvent.HunterTimerPausedEvent -> {
                _phaseState.update { it.copy(playersAreAnswering = true) }
            }

            is GameSessionSocketEvent.HunterTimerResumedEvent -> {
                _phaseState.update {
                    it.copy(
                        playersAreAnswering = false,
                        endTimestamp = event.dto.endTimestamp
                    )
                }
            }

            else -> Unit
        }
    }
}

data class HunterAnsweringResult(val hunterWon: Boolean)