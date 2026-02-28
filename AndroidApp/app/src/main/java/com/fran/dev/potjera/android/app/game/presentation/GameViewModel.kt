package com.fran.dev.potjera.android.app.game.presentation

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.services.BoardPhaseStartingDto
import com.fran.dev.potjera.android.app.game.services.CoinBoosterPlayerStateDto
import com.fran.dev.potjera.android.app.game.services.CoinBoosterQuestionDto
import com.fran.dev.potjera.android.app.game.services.GameResultDto
import com.fran.dev.potjera.android.app.game.services.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.services.GameSessionSocketService
import com.fran.dev.potjera.android.app.game.services.CoinBoosterFinishedDto
import com.fran.dev.potjera.android.app.game.services.CurrentPlayerInfoDto
import com.fran.dev.potjera.android.app.game.services.MoneyOfferAcceptedDto
import com.fran.dev.potjera.android.app.game.services.MoneyOfferDto
import com.fran.dev.potjera.android.app.room.api.RoomPlayerDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.schedulers.Schedulers
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _playersCount = MutableStateFlow<Int>(4)
    var playersCount: StateFlow<Int> = _playersCount.asStateFlow()

    val myPlayerId: Long = prefs.getLong("user_id", 0L)

    private val _gamePhase = MutableStateFlow(GamePhase.STARTING)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _isHunter = MutableStateFlow(false)
    val isHunter: StateFlow<Boolean> = _isHunter.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()
    private val _coinBoosterState = MutableStateFlow<CoinBoosterPlayerStateDto?>(null)
    val coinBoosterState: StateFlow<CoinBoosterPlayerStateDto?> = _coinBoosterState.asStateFlow()

    private val _finishedPlayers = MutableStateFlow<List<CoinBoosterFinishedDto>>(emptyList())
    val finishedPlayers: StateFlow<List<CoinBoosterFinishedDto>> = _finishedPlayers.asStateFlow()

    private val _gameResults = MutableStateFlow<List<GameResultDto>>(emptyList())
    val gameResults: StateFlow<List<GameResultDto>> = _gameResults.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _correctAnswers = MutableStateFlow(0)
    val correctAnswers: StateFlow<Int> = _correctAnswers.asStateFlow()

    private var gameSessionId: String = ""

    private val _coinsBuilt = MutableStateFlow(0)
    val coinsBuilt: StateFlow<Int> = _coinsBuilt.asStateFlow()

    private val _timeLeft = MutableStateFlow(60)
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private var timerJob: Job? = null

    private var coinBoosterEventReceived = false
    private var countdownFinished = false

    fun initGameSession(gameSessionId: String) {
        Log.d(TAG, "initGameSession: $gameSessionId")
        this.gameSessionId = gameSessionId

        val token = prefs.getString("token", null) ?: return

        // connect WebSocket
        gameSessionSocketService.connect(gameSessionId, token)

        // listen to events
        viewModelScope.launch {
            gameSessionSocketService.events.collect { event ->
                handleEvent(event)
            }
        }

        // notify server this player is connected
        //gameSessionSocketService.sendConnect(gameSessionId)
    }

    private fun handleEvent(event: GameSessionSocketEvent) {
        Log.d(TAG, "handleEvent: $event")
        when (event) {
            is GameSessionSocketEvent.CoinBoosterStarted -> {
                _coinBoosterState.value = event.payload.playerState
                _isHunter.value = event.payload.playerState.isHunter
                _isHost.value = event.payload.playerState.isHost

                if (!event.payload.playerState.isHunter) {
                    startTimer(totalSeconds = 60 * event.payload.playerState.questions.size)
                }

                coinBoosterEventReceived = true

                _playersCount.value = event.payload.totalPlayersCount-1

                tryStartCoinBooster(event.payload.playerState.isHunter)
            }

            is GameSessionSocketEvent.CoinBoosterFinished -> {
                _finishedPlayers.update { current ->
                    current + event.payload
                }
            }

            is GameSessionSocketEvent.GameFinished -> {
                _gameResults.value = event.results
                _gamePhase.value = GamePhase.FINISHED
            }

            is GameSessionSocketEvent.BoardPhaseStarting -> {
                _boardState.value = event.state
                _gamePhase.value = GamePhase.BOARD_PHASE
                if (_isHunter.value) {
                    gameSessionSocketService.sendPlayerInfoRequest(gameSessionId)
                }
            }

            is GameSessionSocketEvent.CurrentPlayerInfo -> {
                _currentPlayerInfo.value = event.info
            }

            is GameSessionSocketEvent.MoneyOffer -> {
                _moneyOffer.value = event.offer
            }

            is GameSessionSocketEvent.MoneyOfferAccepted -> {
                _moneyOfferAccepted.value = event.data
            }

            is GameSessionSocketEvent.PlayerLeft -> {
                // handle disconnect if needed
            }
        }
    }

    fun onCountdownFinished() {
        countdownFinished = true
        val isHunter = _coinBoosterState.value?.isHunter ?: false
        tryStartCoinBooster(isHunter)
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

    private fun startTimer(totalSeconds: Int) {
        timerJob?.cancel()
        _timeLeft.value = totalSeconds
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0) {
                delay(1000)
                _timeLeft.update { it - 1 }
            }
            // time ran out — finish with whatever score they have
            finishCoinBooster()
        }
    }

    private fun finishCoinBooster() {
        timerJob?.cancel()
        _gamePhase.value = GamePhase.COIN_BOOSTER_QUEUE
        gameSessionSocketService.sendFinish(gameSessionId, _correctAnswers.value)
    }

    // no timer reset on skipQuestion/submitAnswer anymore
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

    fun nextQuestion(){
        val state = _coinBoosterState.value ?: return
        val nextIndex = _currentQuestionIndex.value + 1
        if (nextIndex >= state.questions.size) {
            finishCoinBooster()
        } else {
            _currentQuestionIndex.value = nextIndex
        }
    }

    // allow 1-2 character mistakes depending on answer length
    private fun isFuzzyMatch(input: String, target: String): Boolean {
        val allowedDistance = when {
            target.length <= 4 -> 0  // short answers must be exact e.g. "Rome"
            target.length <= 7 -> 1  // 1 typo allowed e.g. "Paaris" → "Paris"
            else -> 2  // 2 typos allowed for longer answers
        }
        return levenshtein(input, target) <= allowedDistance
    }

    // Levenshtein distance algorithm
    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(
                        dp[i - 1][j],     // deletion
                        dp[i][j - 1],     // insertion
                        dp[i - 1][j - 1]  // substitution
                    )
                }
            }
        }
        return dp[a.length][b.length]
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        gameSessionSocketService.disconnect()
    }

    // convenience getter for current question
    fun currentQuestion(): CoinBoosterQuestionDto? {
        val state = _coinBoosterState.value ?: return null
        return state.questions.getOrNull(_currentQuestionIndex.value)
    }

    private val _boardState = MutableStateFlow<BoardPhaseStartingDto?>(null)
    val boardState: StateFlow<BoardPhaseStartingDto?> = _boardState.asStateFlow()

    private val _currentPlayerInfo = MutableStateFlow<CurrentPlayerInfoDto?>(null)
    val currentPlayerInfo: StateFlow<CurrentPlayerInfoDto?> = _currentPlayerInfo.asStateFlow()

    private val _moneyOffer = MutableStateFlow<MoneyOfferDto?>(null)
    val moneyOffer: StateFlow<MoneyOfferDto?> = _moneyOffer.asStateFlow()

    private val _moneyOfferAccepted = MutableStateFlow<MoneyOfferAcceptedDto?>(null)
    val moneyOfferAccepted: StateFlow<MoneyOfferAcceptedDto?> = _moneyOfferAccepted.asStateFlow()

    private val _allPlayers = MutableStateFlow<List<CoinBoosterPlayerStateDto>>(emptyList())
    val allPlayers: StateFlow<List<CoinBoosterPlayerStateDto>> = _allPlayers.asStateFlow()

    fun sendMoneyOffer(higherOffer: Float, lowerOffer: Float) {
        gameSessionSocketService.sendMoneyOffer(gameSessionId, higherOffer, lowerOffer)
    }

    fun sendMoneyOfferResponse(acceptedOffer: Float) {
        gameSessionSocketService.sendMoneyOfferResponse(gameSessionId, acceptedOffer)
    }

    fun startBoardQuestions() {
        gameSessionSocketService.sendStartBoardQuestions(gameSessionId)
    }
}

enum class GamePhase {
    STARTING, COIN_BOOSTER, COIN_BOOSTER_QUEUE, BOARD_PHASE,FINISHED
}
