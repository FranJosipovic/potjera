package com.fran.dev.potjera.android.app.game.coinbooster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.AnswerMatcher
import com.fran.dev.potjera.android.app.game.models.CoinBoosterPlayerFinishInfo
import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion
import com.fran.dev.potjera.android.app.game.models.event.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.models.state.CoinBoosterPlayerState
import com.fran.dev.potjera.android.app.game.repository.GameSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.plus

/**
 * Owns: coin booster questions, timer, per-player correct-answer count,
 * finished-players list, and the "queue" waiting screen.
 *
 * The hunter side has no questions — it just waits in the queue screen
 * until the host starts board questions.
 */
@HiltViewModel
class CoinBoosterViewModel @Inject constructor(
    private val repository: GameSessionRepository
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _playerState = MutableStateFlow(CoinBoosterPlayerState())
    val playerState: StateFlow<CoinBoosterPlayerState> = _playerState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _correctAnswers = MutableStateFlow(0)
    val correctAnswers: StateFlow<Int> = _correctAnswers.asStateFlow()

    val coinsBuilt: StateFlow<Int> = _correctAnswers.map { it * 500 }
        .let { flow ->
            // Manually convert without stateIn to avoid extra dependency
            val sf = MutableStateFlow(0)
            viewModelScope.launch { flow.collect { sf.value = it } }
            sf
        }

    private val _timeLeft = MutableStateFlow(60)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _finishedPlayers = MutableStateFlow<List<CoinBoosterPlayerFinishInfo>>(emptyList())
    val finishedPlayers: StateFlow<List<CoinBoosterPlayerFinishInfo>> =
        _finishedPlayers.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var timerJob: Job? = null
    private var gameSessionId: String = ""

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            repository.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    fun setGameSessionId(id: String) {
        gameSessionId = id
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun currentQuestion(): CoinBoosterQuestion? =
        _playerState.value.questions.getOrNull(_currentQuestionIndex.value)

    /**
     * Returns true if the answer was correct.
     */
    fun submitAnswer(answer: String): Boolean {
        val question = currentQuestion() ?: return false
        val correct = AnswerMatcher.isCorrect(answer, question.answer, question.aliases)
        if (correct) _correctAnswers.update { it + 1 }
        return correct
    }

    fun nextQuestion() {
        val nextIndex = _currentQuestionIndex.value + 1
        if (nextIndex >= _playerState.value.questions.size) {
            finish()
        } else {
            _currentQuestionIndex.value = nextIndex
        }
    }

    fun startBoardQuestions() {
        repository.sendStartBoardQuestions(gameSessionId)
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionSocketEvent) {
        when (event) {
            is GameSessionSocketEvent.CoinBoosterStartedPlayerEvent -> {
                _playerState.value = CoinBoosterPlayerState(
                    questions = event.dto.questions,
                    isFinished = false
                )
                _correctAnswers.value = 0
                _currentQuestionIndex.value = 0
                startTimer(60)
            }

            is GameSessionSocketEvent.CoinBoosterStartedHunterEvent -> {
                // Hunter has no questions — nothing to init beyond waiting
            }

            is GameSessionSocketEvent.CoinBoosterFinishedEvent -> {
                _finishedPlayers.update {
                    it + CoinBoosterPlayerFinishInfo(
                        playerId = event.payload.playerId,
                        username = event.payload.username,
                        moneyWon = event.payload.moneyWon
                    )
                }
            }

            else -> Unit
        }
    }

    private fun startTimer(totalSeconds: Int) {
        timerJob?.cancel()
        _timeLeft.value = totalSeconds
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0) {
                delay(1000)
                _timeLeft.update { it - 1 }
            }
            finish()
        }
    }

    private fun finish() {
        timerJob?.cancel()
        _playerState.update { it.copy(isFinished = true) }
        repository.sendFinishCoinBooster(
            gameSessionId = gameSessionId,
            correctAnswers = _correctAnswers.value
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}