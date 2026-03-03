package com.fran.dev.potjera.android.app.game.presentation

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.BoardPhase
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.PlayerVHunterBoardState
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.PlayerVHunterState
import com.fran.dev.potjera.android.app.game.services.CoinBoosterFinishedDto
import com.fran.dev.potjera.android.app.game.services.CoinBoosterPlayerStateDto
import com.fran.dev.potjera.android.app.game.services.CoinBoosterQuestionDto
import com.fran.dev.potjera.android.app.game.services.GameResultDto
import com.fran.dev.potjera.android.app.game.services.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.services.GameSessionSocketService
import com.fran.dev.potjera.android.app.game.services.MoneyOfferDto
import com.fran.dev.potjera.android.app.game.services.PlayerPlayingInfo
import com.fran.dev.potjera.android.app.game.toState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GameViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    private val gameSessionSocketService: GameSessionSocketService
) : ViewModel() {

    companion object {
        private const val TAG = "GameViewModel"
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    val myPlayerId: Long = prefs.getLong("user_id", 0L)

    private var gameSessionId: String = ""

    // ── Session init ──────────────────────────────────────────────────────────

    fun initGameSession(gameSessionId: String) {
        Log.d(TAG, "initGameSession: $gameSessionId")
        this.gameSessionId = gameSessionId

        val token = prefs.getString("token", null) ?: return

        gameSessionSocketService.connect(gameSessionId, token)

        viewModelScope.launch {
            gameSessionSocketService.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    // ── Game phase ────────────────────────────────────────────────────────────

    private val _gamePhase = MutableStateFlow(GamePhase.STARTING)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _isHunter = MutableStateFlow(false)
    val isHunter: StateFlow<Boolean> = _isHunter.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _playersCount = MutableStateFlow(4)
    val playersCount: StateFlow<Int> = _playersCount.asStateFlow()

    private val _allPlayers = MutableStateFlow<List<PlayerPlayingInfo>>(emptyList())
    val allPlayers: StateFlow<List<PlayerPlayingInfo>> = _allPlayers.asStateFlow()

    // ── Countdown ─────────────────────────────────────────────────────────────

    private var coinBoosterEventReceived = false
    private var countdownFinished = false

    fun onCountdownFinished() {
        countdownFinished = true
        tryStartCoinBooster(_coinBoosterState.value?.isHunter ?: false)
    }

    private fun tryStartCoinBooster(isHunter: Boolean) {
        if (coinBoosterEventReceived && countdownFinished) {
            _gamePhase.value = if (isHunter) {
                GamePhase.COIN_BOOSTER_QUEUE
            } else {
                GamePhase.COIN_BOOSTER
            }
        }
    }

    // ── Coin booster phase ────────────────────────────────────────────────────

    private val _coinBoosterState = MutableStateFlow<CoinBoosterPlayerStateDto?>(null)
    val coinBoosterState: StateFlow<CoinBoosterPlayerStateDto?> = _coinBoosterState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _correctAnswers = MutableStateFlow(0)
    val correctAnswers: StateFlow<Int> = _correctAnswers.asStateFlow()

    private val _coinsBuilt = MutableStateFlow(0)
    val coinsBuilt: StateFlow<Int> = _coinsBuilt.asStateFlow()

    private val _timeLeft = MutableStateFlow(60)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _gameEvent = Channel<GameEvent>(Channel.BUFFERED)
    val gameEvent: Flow<GameEvent> = _gameEvent.receiveAsFlow()

    private var timerJob: Job? = null

    fun currentQuestion(): CoinBoosterQuestionDto? {
        return _coinBoosterState.value?.questions?.getOrNull(_currentQuestionIndex.value)
    }

    fun submitAnswer(answer: String): Boolean {
        val state = _coinBoosterState.value ?: return false
        val question = state.questions.getOrNull(_currentQuestionIndex.value) ?: return false

        val normalizedInput = answer.trim().lowercase()
        val normalizedAnswer = question.answer.trim().lowercase()
        val normalizedAliases = question.aliases.map { it.trim().lowercase() }

        val isCorrect = normalizedInput == normalizedAnswer ||
                normalizedAliases.any { it == normalizedInput } ||
                isFuzzyMatch(normalizedInput, normalizedAnswer) ||
                normalizedAliases.any { isFuzzyMatch(normalizedInput, it) }

        if (isCorrect) {
            _correctAnswers.update { it + 1 }
            _coinsBuilt.update { it + 500 }
        }

        return isCorrect
    }

    fun nextQuestion() {
        val state = _coinBoosterState.value ?: return
        val nextIndex = _currentQuestionIndex.value + 1
        if (nextIndex >= state.questions.size) {
            finishCoinBooster()
        } else {
            _currentQuestionIndex.value = nextIndex
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
            finishCoinBooster()
        }
    }

    private fun finishCoinBooster() {
        timerJob?.cancel()
        _gamePhase.value = GamePhase.COIN_BOOSTER_QUEUE
        gameSessionSocketService.sendFinish(gameSessionId, _correctAnswers.value)
    }

    // ── Coin booster queue ────────────────────────────────────────────────────

    private val _finishedPlayers = MutableStateFlow<List<CoinBoosterFinishedDto>>(emptyList())
    val finishedPlayers: StateFlow<List<CoinBoosterFinishedDto>> = _finishedPlayers.asStateFlow()

    fun startBoardQuestions() {
        gameSessionSocketService.sendStartBoardQuestions(gameSessionId)
    }

    // ── Board phase ───────────────────────────────────────────────────────────

    private val _playerVHunterGlobalState = MutableStateFlow<PlayerVHunterState?>(null)
    val playerVHunterGlobalState: StateFlow<PlayerVHunterState?> =
        _playerVHunterGlobalState.asStateFlow()

    private val _playerVHunterBoardState = MutableStateFlow<PlayerVHunterBoardState?>(null)
    val playerVHunterBoardState: StateFlow<PlayerVHunterBoardState?> =
        _playerVHunterBoardState.asStateFlow()

    private val _moneyOffer = MutableStateFlow<MoneyOfferDto?>(null)
    val moneyOffer: StateFlow<MoneyOfferDto?> = _moneyOffer.asStateFlow()

    fun sendMoneyOffer(higherOffer: Float, lowerOffer: Float) {
        gameSessionSocketService.sendMoneyOffer(gameSessionId, higherOffer, lowerOffer)
    }

    fun sendMoneyOfferResponse(acceptedOffer: Float) {
        gameSessionSocketService.sendMoneyOfferResponse(gameSessionId, acceptedOffer)
    }

    fun sendBoardAnswer(answer: String) {
        gameSessionSocketService.sendBoardAnswer(gameSessionId, answer, _isHunter.value)
    }

    // ── End game ──────────────────────────────────────────────────────────────

    private val _gameResults = MutableStateFlow<List<GameResultDto>>(emptyList())
    val gameResults: StateFlow<List<GameResultDto>> = _gameResults.asStateFlow()

    // ── Event handler ─────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionSocketEvent) {
        Log.d(TAG, "handleEvent: $event")
        when (event) {
            is GameSessionSocketEvent.CoinBoosterStarted -> {
                _coinBoosterState.value = event.payload.playerState
                _isHunter.value = event.payload.playerState.isHunter
                _isHost.value = event.payload.playerState.isHost
                _playersCount.value = event.payload.totalPlayersCount - 1

                if (!event.payload.playerState.isHunter) {
                    startTimer(60 * event.payload.playerState.questions.size)
                }
                coinBoosterEventReceived = true
                tryStartCoinBooster(event.payload.playerState.isHunter)
            }

            is GameSessionSocketEvent.CoinBoosterFinished -> {
                _finishedPlayers.update { it + event.payload }
            }

            is GameSessionSocketEvent.BoardPhaseStarting -> {
                _playerVHunterGlobalState.value = event.dto.globalState.toState()
                _playerVHunterBoardState.value = event.dto.boardState.toState()
                _allPlayers.value = event.dto.globalState.players.map {
                    PlayerPlayingInfo(it.key, it.value)
                }
                _moneyOffer.value = null
                _gamePhase.value = GamePhase.BOARD_PHASE
            }

            is GameSessionSocketEvent.MoneyOffer -> {
                _moneyOffer.value = event.dto
                _playerVHunterBoardState.value =
                    _playerVHunterBoardState.value?.copy(boardPhase = BoardPhase.PLAYER_CHOOSING)
            }
            is GameSessionSocketEvent.MoneyOfferAccepted -> {
                _moneyOffer.value = null
                _playerVHunterBoardState.value = event.dto.toState()
            }

            is GameSessionSocketEvent.BoardQuestionUpdate -> {
                _playerVHunterBoardState.value = event.dto.toState()
            }

            is GameSessionSocketEvent.AnswerRevealed -> {
                viewModelScope.launch {
                    val newState = event.dto.toState()
                    _playerVHunterBoardState.update { it?.copy(boardPhase = BoardPhase.ANSWER_REVEAL) }
                    delay(1000)
                    _playerVHunterBoardState.update { it?.copy(playerCorrectAnswers = newState.playerCorrectAnswers) }
                    delay(1000)
                    _playerVHunterBoardState.update { newState }
                }
            }

            is GameSessionSocketEvent.PlayerWon -> {
                _playerVHunterGlobalState.update { event.dto.toState() }
                val state = event.dto
                val username = state.players[state.currentPlayerId] ?: "Unknown"
                val money = state.playersFinishStatus[state.currentPlayerId] ?: 0f
                viewModelScope.launch { _gameEvent.send(GameEvent.PlayerWon(username, money)) }
            }

            is GameSessionSocketEvent.PlayerCaught -> {
                _playerVHunterGlobalState.update { event.dto.toState() }
                val state = event.dto
                val username = state.players[state.currentPlayerId] ?: "Unknown"
                viewModelScope.launch { _gameEvent.send(GameEvent.PlayerCaught(username)) }
            }

            is GameSessionSocketEvent.BoardPhaseFinished -> {
                _playerVHunterGlobalState.update { event.dto.toState() }
                viewModelScope.launch { _gameEvent.send(GameEvent.BoardPhaseFinished) }
            }

            is GameSessionSocketEvent.GameFinished -> {
                _gameResults.value = event.results
                _gamePhase.value = GamePhase.FINISHED
            }

            is GameSessionSocketEvent.PlayerLeft -> {}
        }
    }

    // ── Fuzzy matching ────────────────────────────────────────────────────────

    private fun isFuzzyMatch(input: String, target: String): Boolean {
        val allowedDistance = when {
            target.length <= 4 -> 0
            target.length <= 7 -> 1
            else -> 2
        }
        return levenshtein(input, target) <= allowedDistance
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        gameSessionSocketService.disconnect()
    }
}

enum class GamePhase {
    STARTING,
    COIN_BOOSTER,
    COIN_BOOSTER_QUEUE,
    BOARD_PHASE,
    FINISHED
}

sealed class GameEvent {
    data class PlayerWon(val username: String, val money: Float) : GameEvent()
    data class PlayerCaught(val username: String) : GameEvent()
    object BoardPhaseFinished : GameEvent()
}