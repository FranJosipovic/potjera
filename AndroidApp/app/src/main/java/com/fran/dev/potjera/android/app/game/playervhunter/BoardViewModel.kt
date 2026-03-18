package com.fran.dev.potjera.android.app.game.playervhunter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.di.GameSessionRepositoryFactory
import com.fran.dev.potjera.android.app.game.models.BoardQuestion
import com.fran.dev.potjera.android.app.game.models.MoneyOffer
import com.fran.dev.potjera.android.app.game.models.enums.BoardPhase
import com.fran.dev.potjera.android.app.game.models.event.GameSessionEvent
import com.fran.dev.potjera.android.app.game.models.state.PlayerVHunterBoardState
import com.fran.dev.potjera.android.app.game.repository.Difficulty
import com.fran.dev.potjera.android.app.game.repository.GameSessionRepository
import com.fran.dev.potjera.android.app.game.toState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
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
    private val repositoryFactory: GameSessionRepositoryFactory
) : ViewModel() {

    companion object {
        private const val TAG = "BoardViewModel"
    }

    private var eventsJob: Job? = null
    private lateinit var repository: GameSessionRepository

    // ── State ─────────────────────────────────────────────────────────────────

    private val _boardState = MutableStateFlow<PlayerVHunterBoardState?>(null)
    val boardState: StateFlow<PlayerVHunterBoardState?> = _boardState.asStateFlow()

    private val _moneyOffer = MutableStateFlow<MoneyOffer?>(null)
    val moneyOffer: StateFlow<MoneyOffer?> = _moneyOffer.asStateFlow()

    private val _currentPlayerId = MutableStateFlow<Long?>(null)
    val currentPlayerId: StateFlow<Long?> = _currentPlayerId.asStateFlow()

    private val _boardEvent = MutableStateFlow<BoardEvent?>(null)
    val boardEvent: StateFlow<BoardEvent?> = _boardEvent.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var gameSessionId: String = ""
    private var isHunter: Boolean = false

    // ── Init ──────────────────────────────────────────────────────────────────

    fun init(gameSessionId: String, difficulty: Difficulty?, isHunter: Boolean) {
        Log.d(TAG, "init: gameSessionId=$gameSessionId difficulty=$difficulty isHunter=$isHunter")
        this.gameSessionId = gameSessionId
        this.isHunter = isHunter
        repository = repositoryFactory.get(difficulty)

        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            repository.events.collect { event -> handleEvent(event) }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun sendMoneyOffer(higherOffer: Float, lowerOffer: Float) {
        Log.d(TAG, "sendMoneyOffer: higher=\$$higherOffer lower=\$$lowerOffer")
        viewModelScope.launch { repository.sendMoneyOffer(higherOffer, lowerOffer) }
    }

    fun sendMoneyOfferResponse(acceptedOffer: Float) {
        Log.i(TAG, "sendMoneyOfferResponse: accepted=\$$acceptedOffer")
        viewModelScope.launch { repository.sendMoneyOfferResponse(acceptedOffer) }
    }

    fun sendBoardAnswer(answer: String) {
        Log.d(TAG, "sendBoardAnswer: answer=\"$answer\" isHunter=$isHunter")
        viewModelScope.launch { repository.sendBoardAnswer(answer, isHunter) }
    }

    fun consumeBoardEvent() {
        Log.d(TAG, "consumeBoardEvent: clearing ${_boardEvent.value?.let { it::class.simpleName }}")
        _boardEvent.value = null
    }

    fun startPlayersAnsweringPhase() {
        Log.i(TAG, "startPlayersAnsweringPhase")
        viewModelScope.launch { repository.startPlayersAnsweringPhase() }
    }

    // ── Event handler ─────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionEvent) {
        when (event) {
            is GameSessionEvent.BoardPhaseStartingEvent -> {
                Log.i(TAG, "BoardPhaseStarting: currentPlayer=${event.dto.currentPlayerId}" +
                        " moneyInGame=\$${event.dto.boardState.moneyInGame}" +
                        " phase=${event.dto.boardState.boardPhase}")
                _currentPlayerId.update { event.dto.currentPlayerId }
                _boardState.update { event.dto.boardState.toState() }
                _moneyOffer.update { null }
            }

            is GameSessionEvent.MoneyOfferEvent -> {
                Log.i(TAG, "MoneyOffer: higher=\$${"%.0f".format(event.dto.higherOffer)}" +
                        " lower=\$${"%.0f".format(event.dto.lowerOffer)}")
                _moneyOffer.update {
                    MoneyOffer(higherOffer = event.dto.higherOffer, lowerOffer = event.dto.lowerOffer)
                }
                _boardState.update { it?.copy(boardPhase = BoardPhase.PLAYER_CHOOSING) }
            }

            is GameSessionEvent.MoneyOfferAcceptedEvent -> {
                Log.i(TAG, "MoneyOfferAccepted: moneyInGame=\$${event.dto.moneyInGame}" +
                        " playerStartingIndex=${event.dto.playerStartingIndex}")
                _moneyOffer.update { null }
                _boardState.update {
                    it?.copy(
                        boardPhase          = BoardPhase.OFFER_ACCEPTED,
                        playerStartingIndex = event.dto.playerStartingIndex,
                        moneyInGame         = event.dto.moneyInGame
                    )
                }
            }

            is GameSessionEvent.NewBoardQuestionEvent -> {
                Log.d(TAG, "NewBoardQuestion: \"${event.dto.question}\"" +
                        " choices=${event.dto.choices}")
                _boardState.update {
                    it?.copy(
                        boardPhase       = BoardPhase.QUESTION_READING,
                        questionsStarted = true,
                        boardQuestion    = BoardQuestion(
                            question      = event.dto.question,
                            choices       = event.dto.choices,
                            correctAnswer = event.dto.correctAnswer
                        ),
                        playerAnswer = null,
                        hunterAnswer = null
                    )
                }
            }

            is GameSessionEvent.PlayerAnsweredQuestionEvent -> {
                Log.d(TAG, "PlayerAnswered: \"${event.dto.answer}\"")
                _boardState.update {
                    it?.copy(playerAnswer = event.dto.answer, boardPhase = BoardPhase.ANSWER_GIVEN)
                }
            }

            is GameSessionEvent.HunterAnsweredQuestionEvent -> {
                Log.d(TAG, "HunterAnswered: \"${event.dto.answer}\"")
                _boardState.update {
                    it?.copy(hunterAnswer = event.dto.answer, boardPhase = BoardPhase.ANSWER_GIVEN)
                }
            }

            is GameSessionEvent.AnswerRevealedEvent -> {
                Log.i(TAG, "AnswerRevealed: playerCorrect=${event.dto.playerAnsweredCorrectly}" +
                        " hunterCorrect=${event.dto.hunterAnsweredCorrectly}")
                viewModelScope.launch {
                    _boardState.update { it?.copy(boardPhase = BoardPhase.ANSWER_REVEAL) }
                    if (event.dto.playerAnsweredCorrectly) {
                        delay(1_000)
                        _boardState.update { it?.copy(playerCorrectAnswers = it.playerCorrectAnswers + 1) }
                        Log.d(TAG, "AnswerRevealed: playerCorrectAnswers=${_boardState.value?.playerCorrectAnswers}")
                    }
                    if (event.dto.hunterAnsweredCorrectly) {
                        delay(1_000)
                        _boardState.update { it?.copy(hunterCorrectAnswers = it.hunterCorrectAnswers + 1) }
                        Log.d(TAG, "AnswerRevealed: hunterCorrectAnswers=${_boardState.value?.hunterCorrectAnswers}")
                    }
                }
            }

            is GameSessionEvent.PlayerWonEvent -> {
                val username = event.dto.playersListUpdated[event.dto.playerWonId]?.playerName ?: "unknown"
                Log.i(TAG, "PlayerWon: $username won \$${event.dto.moneyWon}")
                _boardEvent.value = BoardEvent.PlayerWon(username, event.dto.moneyWon)
            }

            is GameSessionEvent.PlayerCaughtEvent -> {
                val username = event.dto.playersListUpdated[event.dto.playerCaughtId]?.playerName ?: "unknown"
                Log.i(TAG, "PlayerCaught: $username was caught")
                _boardEvent.value = BoardEvent.PlayerCaught(username)
            }

            is GameSessionEvent.BoardPhaseFinishedEvent -> {
                Log.i(TAG, "BoardPhaseFinished")
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