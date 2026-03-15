package com.fran.dev.potjera.android.app.game.playervhunter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.models.BoardQuestion
import com.fran.dev.potjera.android.app.game.models.MoneyOffer
import com.fran.dev.potjera.android.app.game.models.enums.BoardPhase
import com.fran.dev.potjera.android.app.game.models.event.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.models.state.PlayerVHunterBoardState
import com.fran.dev.potjera.android.app.game.repository.GameSessionRepository
import com.fran.dev.potjera.android.app.game.toState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns: board ladder state, money offer, current answering player.
 * Knows nothing about coin booster, players-answering, or hunter-answering.
 */
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val repository: GameSessionRepository
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _boardState = MutableStateFlow<PlayerVHunterBoardState?>(null)
    val boardState: StateFlow<PlayerVHunterBoardState?> = _boardState.asStateFlow()

    private val _moneyOffer = MutableStateFlow<MoneyOffer?>(null)
    val moneyOffer: StateFlow<MoneyOffer?> = _moneyOffer.asStateFlow()

    private val _currentPlayerId = MutableStateFlow<Long?>(null)
    val currentPlayerId: StateFlow<Long?> = _currentPlayerId.asStateFlow()

    // one-shot events (player won / caught / board finished) for dialogs
    private val _boardEvent = MutableStateFlow<BoardEvent?>(null)
    val boardEvent: StateFlow<BoardEvent?> = _boardEvent.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var gameSessionId: String = ""
    private var isHunter: Boolean = false

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repository.events.collect { event -> handleEvent(event) }
        }
    }

    fun setContext(gameSessionId: String, isHunter: Boolean) {
        this.gameSessionId = gameSessionId
        this.isHunter = isHunter
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun sendMoneyOffer(higherOffer: Float, lowerOffer: Float) {
        repository.sendMoneyOffer(gameSessionId, higherOffer, lowerOffer)
    }

    fun sendMoneyOfferResponse(acceptedOffer: Float) {
        repository.sendMoneyOfferResponse(gameSessionId, acceptedOffer)
    }

    fun sendBoardAnswer(answer: String) {
        repository.sendBoardAnswer(gameSessionId, answer, isHunter)
    }

    fun consumeBoardEvent() {
        _boardEvent.value = null
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionSocketEvent) {
        when (event) {
            is GameSessionSocketEvent.BoardPhaseStartingEvent -> {
                _currentPlayerId.update { event.dto.currentPlayerId }
                _boardState.update { event.dto.boardState.toState() }
                _moneyOffer.update { null }
            }

            is GameSessionSocketEvent.MoneyOfferEvent -> {
                _moneyOffer.update {
                    MoneyOffer(
                        higherOffer = event.dto.higherOffer,
                        lowerOffer = event.dto.lowerOffer
                    )
                }
                _boardState.update {
                    PlayerVHunterBoardState(boardPhase = BoardPhase.PLAYER_CHOOSING)
                }
            }

            is GameSessionSocketEvent.MoneyOfferAcceptedEvent -> {
                _moneyOffer.update { null }
                _boardState.update {
                    it?.copy(
                        boardPhase = BoardPhase.OFFER_ACCEPTED,
                        playerStartingIndex = event.dto.playerStartingIndex,
                        moneyInGame = event.dto.moneyInGame
                    )
                }
            }

            is GameSessionSocketEvent.NewBoardQuestionEvent -> {
                _boardState.update {
                    it?.copy(
                        boardPhase = BoardPhase.QUESTION_READING,
                        questionsStarted = true,
                        boardQuestion = BoardQuestion(
                            question = event.dto.question,
                            choices = event.dto.choices,
                            correctAnswer = event.dto.correctAnswer
                        ),
                        playerAnswer = null,
                        hunterAnswer = null
                    )
                }
            }

            is GameSessionSocketEvent.PlayerAnsweredQuestionEvent -> {
                _boardState.update {
                    it?.copy(playerAnswer = event.dto.answer, boardPhase = BoardPhase.ANSWER_GIVEN)
                }
            }

            is GameSessionSocketEvent.HunterAnsweredQuestionEvent -> {
                _boardState.update {
                    it?.copy(hunterAnswer = event.dto.answer, boardPhase = BoardPhase.ANSWER_GIVEN)
                }
            }

            is GameSessionSocketEvent.AnswerRevealedEvent -> {
                viewModelScope.launch {
                    _boardState.update { it?.copy(boardPhase = BoardPhase.ANSWER_REVEAL) }
                    if (event.dto.playerAnsweredCorrectly) {
                        delay(1000)
                        _boardState.update {
                            it?.copy(playerCorrectAnswers = it.playerCorrectAnswers + 1)
                        }
                    }
                    if (event.dto.hunterAnsweredCorrectly) {
                        delay(1000)
                        _boardState.update {
                            it?.copy(hunterCorrectAnswers = it.hunterCorrectAnswers + 1)
                        }
                    }
                }
            }

            is GameSessionSocketEvent.PlayerWonEvent -> {
                val username =
                    event.dto.playersListUpdated[event.dto.playerWonId]?.playerName ?: ""
                _boardEvent.value = BoardEvent.PlayerWon(username, event.dto.moneyWon)
            }

            is GameSessionSocketEvent.PlayerCaughtEvent -> {
                val username =
                    event.dto.playersListUpdated[event.dto.playerCaughtId]?.playerName ?: ""
                _boardEvent.value = BoardEvent.PlayerCaught(username)
            }

            is GameSessionSocketEvent.BoardPhaseFinishedEvent -> {
                _boardEvent.value = BoardEvent.BoardPhaseFinished
            }

            else -> Unit
        }
    }
}

// ── Board-scoped one-shot events ──────────────────────────────────────────────

sealed interface BoardEvent {
    data class PlayerWon(val username: String, val money: Float) : BoardEvent
    data class PlayerCaught(val username: String) : BoardEvent
    data object BoardPhaseFinished : BoardEvent
}