package com.fran.dev.potjera.android.app.game.presentation

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fran.dev.potjera.android.app.game.hunterphase.presentation.SuggestionItem
import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.game.models.dto.board.MoneyOfferDto
import com.fran.dev.potjera.android.app.game.models.enums.GamePhase
import com.fran.dev.potjera.android.app.game.models.state.CoinBoosterPlayerState
import com.fran.dev.potjera.android.app.game.models.state.GameSessionState
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.BoardPhase
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.BoardQuestion
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.PlayerVHunterBoardState
import com.fran.dev.potjera.android.app.game.presentation.GameEvent.BoardPhaseFinished
import com.fran.dev.potjera.android.app.game.presentation.GameEvent.PlayerCaught
import com.fran.dev.potjera.android.app.game.presentation.GameEvent.PlayerWon
import com.fran.dev.potjera.android.app.game.presentation.GameEvent.PlayersAnsweringFinished
import com.fran.dev.potjera.android.app.game.services.CoinBoosterFinishedDto
import com.fran.dev.potjera.android.app.game.services.GameResultDto
import com.fran.dev.potjera.android.app.game.services.GameSessionSocketEvent
import com.fran.dev.potjera.android.app.game.services.GameSessionSocketService
import com.fran.dev.potjera.android.app.game.toState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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

    // ── Session init ──────────────────────────────────────────────────────────

    private var _gameSessionState = MutableStateFlow(GameSessionState())
    val gameSessionState: StateFlow<GameSessionState> = _gameSessionState

    fun initGameSession(gameSessionId: String) {
        Log.d(TAG, "initGameSession: $gameSessionId")
        _gameSessionState.update { it.copy(gameSessionId = gameSessionId) }

        val token = prefs.getString("token", null) ?: return

        gameSessionSocketService.connect(gameSessionId, token)

        viewModelScope.launch {
            gameSessionSocketService.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    // ── Game phase ────────────────────────────────────────────────────────────

    private val _isHunter = MutableStateFlow(false)
    val isHunter: StateFlow<Boolean> = _isHunter.asStateFlow()

    val isCaptain: StateFlow<Boolean> = _gameSessionState.map {
        it.gameSessionPlayers[myPlayerId]?.isCaptain == true
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    val captainId: StateFlow<Long?> = _gameSessionState.map { state ->
        state.gameSessionPlayers.values.firstOrNull { it.isCaptain }?.playerId
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    val isSpectator: StateFlow<Boolean> = _gameSessionState.map {
        it.gameSessionPlayers[myPlayerId]?.isEliminated == true
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )


    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    val players: StateFlow<List<GameSessionPlayer>> = _gameSessionState
        .map { it.gameSessionPlayers.values.toList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val hunterId =
        players.map { player -> player.firstOrNull { it.isHunter }?.playerId ?: 0L }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0L
        )


    // ── Countdown ─────────────────────────────────────────────────────────────

    private var coinBoosterEventReceived = false
    private var countdownFinished = false

    fun onCountdownFinished() {
        countdownFinished = true
        tryStartCoinBooster(isHunter.value)
    }

    private fun tryStartCoinBooster(isHunter: Boolean) {
        if (isHunter) {
            if (countdownFinished) {
                _gameSessionState.update {
                    it.copy(
                        gamePhase = GamePhase.COIN_BOOSTER
                    )
                }
            }
        } else {
            if (coinBoosterEventReceived && countdownFinished) {
                _gameSessionState.update {
                    it.copy(
                        gamePhase = GamePhase.COIN_BOOSTER
                    )
                }
            }
        }
    }

    // ── Coin booster phase ────────────────────────────────────────────────────

    private val _coinBoosterState =
        MutableStateFlow<CoinBoosterPlayerState>(CoinBoosterPlayerState())
    val coinBoosterState: StateFlow<CoinBoosterPlayerState> = _coinBoosterState.asStateFlow()

    private val _currentCoinBoosterQuestionIndex = MutableStateFlow(0)
    val currentCoinBoosterQuestionIndex: StateFlow<Int> =
        _currentCoinBoosterQuestionIndex.asStateFlow()

    private val _coinBoosterCorrectAnswers = MutableStateFlow(0)
    val coinBoosterCorrectAnswers: StateFlow<Int> = _coinBoosterCorrectAnswers.asStateFlow()

    val coinsBuilt: StateFlow<Int> = _coinBoosterCorrectAnswers.map { it * 500 }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0
    )

    private val _coinBoosterTimeLeft = MutableStateFlow(60)
    val coinBoosterTimeLeft: StateFlow<Int> = _coinBoosterTimeLeft.asStateFlow()

    private val _coinBoosterFinishedPlayersList =
        MutableStateFlow<List<CoinBoosterFinishedDto>>(emptyList())
    val coinBoosterFinishedPlayersList: StateFlow<List<CoinBoosterFinishedDto>> =
        _coinBoosterFinishedPlayersList.asStateFlow()

    fun startBoardQuestions() {
        gameSessionSocketService.sendStartBoardQuestions(_gameSessionState.value.gameSessionId)
    }

    // ---------------------------------------------------------_________________

    private val _gameEvent = Channel<GameEvent>(Channel.BUFFERED)
    val gameEvent: Flow<GameEvent> = _gameEvent.receiveAsFlow()

    // ── Players answering phase ──────────────────────────────────────────────────────────────

    private val _currentAnsweringPlayerId = MutableStateFlow<Long?>(null)
    val currentAnsweringPlayerId: StateFlow<Long?> = _currentAnsweringPlayerId.asStateFlow()

    private val _playersAnsweringPlayerList =
        MutableStateFlow<List<PlayersAnsweringPlayer>>(emptyList())
    val playersAnsweringPlayerList: StateFlow<List<PlayersAnsweringPlayer>> =
        _playersAnsweringPlayerList

    private val _playerAnsweredCorrectly = MutableStateFlow<Boolean?>(null)
    val playerAnsweredCorrectly: StateFlow<Boolean?> = _playerAnsweredCorrectly.asStateFlow()

    private val _totalSteps = MutableStateFlow(0)
    val totalSteps: StateFlow<Int> = _totalSteps

    private val _questionText = MutableStateFlow<String?>(null)
    val questionText: StateFlow<String?> = _questionText

    private val _correctAnswer = MutableStateFlow<String?>(null)
    val correctAnswer: StateFlow<String?> = _correctAnswer.asStateFlow()

    fun buzzIn() {
        gameSessionSocketService.sendBuzzIn(_gameSessionState.value.gameSessionId)
    }

    fun answerQuestion(answer: String) {
        gameSessionSocketService.sendPlayersAnsweringAnswer(
            _gameSessionState.value.gameSessionId,
            answer
        )
    }

    private val phaseEmojis = listOf("🎮", "🧠", "⚡", "🎯")

    private var timerJob: Job? = null

    fun currentQuestion(): CoinBoosterQuestion? {
        return _coinBoosterState.value.questions.getOrNull(_currentCoinBoosterQuestionIndex.value)
    }

    fun submitAnswer(answer: String): Boolean {
        val state = _coinBoosterState.value ?: return false
        val question =
            state.questions.getOrNull(_currentCoinBoosterQuestionIndex.value) ?: return false

        val normalizedInput = answer.trim().lowercase()
        val normalizedAnswer = question.answer.trim().lowercase()
        val normalizedAliases = question.aliases.map { it.trim().lowercase() }

        val isCorrect = normalizedInput == normalizedAnswer ||
                normalizedAliases.any { it == normalizedInput } ||
                isFuzzyMatch(normalizedInput, normalizedAnswer) ||
                normalizedAliases.any { isFuzzyMatch(normalizedInput, it) }

        if (isCorrect) {
            _coinBoosterCorrectAnswers.update { it + 1 }
        }

        return isCorrect
    }

    fun nextQuestion() {
        val state = _coinBoosterState.value ?: return
        val nextIndex = _currentCoinBoosterQuestionIndex.value + 1
        if (nextIndex >= state.questions.size) {
            finishCoinBooster()
        } else {
            _currentCoinBoosterQuestionIndex.value = nextIndex
        }
    }

    private fun startTimer(totalSeconds: Int) {
        timerJob?.cancel()
        _coinBoosterTimeLeft.value = totalSeconds
        timerJob = viewModelScope.launch {
            while (_coinBoosterTimeLeft.value > 0) {
                delay(1000)
                _coinBoosterTimeLeft.update { it - 1 }
            }
            finishCoinBooster()
        }
    }

    private fun finishCoinBooster() {
        timerJob?.cancel()
        gameSessionSocketService.sendFinish(
            gameSessionId = _gameSessionState.value.gameSessionId,
            correctAnswers = _coinBoosterCorrectAnswers.value
        )
        _coinBoosterState.update { it.copy(isFinished = true) }
    }

    // ── Coin booster queue ────────────────────────────────────────────────────


    // ── Board phase ───────────────────────────────────────────────────────────

    private val _boardPhaseCurrentPlayerId = MutableStateFlow<Long?>(null)
    val boardPhaseCurrentPlayerId = _boardPhaseCurrentPlayerId.asStateFlow()

    private val _playerVHunterBoardState = MutableStateFlow<PlayerVHunterBoardState?>(null)
    val playerVHunterBoardState: StateFlow<PlayerVHunterBoardState?> =
        _playerVHunterBoardState.asStateFlow()

    private val _moneyOfferDto = MutableStateFlow<MoneyOfferDto?>(null)
    val moneyOfferDto: StateFlow<MoneyOfferDto?> = _moneyOfferDto.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SuggestionItem>>(emptyList<SuggestionItem>())
    val suggestions: StateFlow<List<SuggestionItem>> = _suggestions.asStateFlow()

    fun sendMoneyOffer(higherOffer: Float, lowerOffer: Float) {
        gameSessionSocketService.sendMoneyOffer(
            _gameSessionState.value.gameSessionId,
            higherOffer,
            lowerOffer
        )
    }

    fun sendMoneyOfferResponse(acceptedOffer: Float) {
        gameSessionSocketService.sendMoneyOfferResponse(
            _gameSessionState.value.gameSessionId,
            acceptedOffer
        )
    }

    fun sendBoardAnswer(answer: String) {
        gameSessionSocketService.sendBoardAnswer(
            _gameSessionState.value.gameSessionId,
            answer,
            _isHunter.value
        )
    }

    fun sendSuggestion(suggestion: String) {
        gameSessionSocketService.sendSuggestion(
            _gameSessionState.value.gameSessionId,
            suggestion
        )
    }

    // ── Hunter Answering Phase ──────────────────────────────────────────────────────────────

    private val _hunterAnsweringPhaseState =
        MutableStateFlow<HunterAnsweringPhaseState>(HunterAnsweringPhaseState())
    val hunterAnsweringPhaseState: StateFlow<HunterAnsweringPhaseState> =
        _hunterAnsweringPhaseState.asStateFlow()

    fun sendHunterAnswer(answer: String) {
        gameSessionSocketService.sendHunterAnsweringAnswer(
            _gameSessionState.value.gameSessionId,
            answer
        )
    }

    fun sendPlayersAnswer(answer: String) {
        gameSessionSocketService.sendPlayerCounterAnswer(
            _gameSessionState.value.gameSessionId,
            answer
        )
    }


    // ── End game ──────────────────────────────────────────────────────────────

    private val _gameResults = MutableStateFlow<List<GameResultDto>>(emptyList())
    val gameResults: StateFlow<List<GameResultDto>> = _gameResults.asStateFlow()

    // ── Event handler ─────────────────────────────────────────────────────────

    private fun handleEvent(event: GameSessionSocketEvent) {
        Log.d(TAG, "handleEvent: $event")
        when (event) {
            is GameSessionSocketEvent.CoinBoosterStartedHunterEvent -> {
                _isHunter.update { true }
                _isHost.update { event.dto.playersInfo.values.firstOrNull { it.isHost }?.playerId == myPlayerId }

                _gameSessionState.update {
                    it.copy(
                        gameSessionPlayers = event.dto.playersInfo
                    )
                }

                coinBoosterEventReceived = true
                tryStartCoinBooster(isHunter = true)
            }

            is GameSessionSocketEvent.CoinBoosterStartedPlayerEvent -> {
                _coinBoosterState.value = CoinBoosterPlayerState(
                    playerId = myPlayerId,
                    correctAnswers = 0,
                    questions = event.dto.questions,
                    isFinished = false
                )
                _isHunter.update { false }
                _isHost.update { event.dto.playersInfo.values.firstOrNull { it.isHost }?.playerId == myPlayerId }

                _gameSessionState.update {
                    it.copy(
                        gameSessionPlayers = event.dto.playersInfo
                    )
                }

                startTimer(totalSeconds = 60)

                coinBoosterEventReceived = true
                tryStartCoinBooster(isHunter = false)
            }

            is GameSessionSocketEvent.CoinBoosterFinishedEvent -> {
                _coinBoosterFinishedPlayersList.update { it + event.payload }
            }

            //ALSO CALLED WHEN MOVED TO NEXT PLAYER
            is GameSessionSocketEvent.BoardPhaseStartingEvent -> {
                _boardPhaseCurrentPlayerId.update { event.dto.currentPlayerId }
                _playerVHunterBoardState.update { event.dto.boardState.toState() }
                _moneyOfferDto.update { null }
                _gameSessionState.update {
                    it.copy(
                        gamePhase = GamePhase.BOARD
                    )
                }
            }

            is GameSessionSocketEvent.MoneyOfferEvent -> {
                _moneyOfferDto.update { event.dto }
                _playerVHunterBoardState.update {
                    PlayerVHunterBoardState(boardPhase = BoardPhase.PLAYER_CHOOSING)
                }
            }

            is GameSessionSocketEvent.MoneyOfferAcceptedEvent -> {
                _moneyOfferDto.update { null }
                _playerVHunterBoardState.update {
                    it?.copy(
                        boardPhase = BoardPhase.OFFER_ACCEPTED,
                        playerStartingIndex = event.dto.playerStartingIndex,
                        moneyInGame = event.dto.moneyInGame
                    )
                }
            }

            is GameSessionSocketEvent.NewBoardQuestionEvent -> {
                _playerVHunterBoardState.update {
                    it?.copy(
                        boardPhase = BoardPhase.QUESTION_READING,
                        questionsStarted = true,
                        boardQuestion = BoardQuestion(
                            question = event.dto.question,
                            choices = event.dto.choices,
                            correctAnswer = event.dto.correctAnswer
                        ),
                        playerAnswer = null,
                        hunterAnswer = null,
                    )
                }
            }

            is GameSessionSocketEvent.PlayerAnsweredQuestionEvent -> {
                _playerVHunterBoardState.update {
                    it?.copy(playerAnswer = event.dto.answer, boardPhase = BoardPhase.ANSWER_GIVEN)
                }
            }

            is GameSessionSocketEvent.HunterAnsweredQuestionEvent -> {
                _playerVHunterBoardState.update {
                    it?.copy(hunterAnswer = event.dto.answer, boardPhase = BoardPhase.ANSWER_GIVEN)
                }
            }

            is GameSessionSocketEvent.AnswerRevealedEvent -> {
                viewModelScope.launch {
                    _playerVHunterBoardState.update { it?.copy(boardPhase = BoardPhase.ANSWER_REVEAL) }
                    if (event.dto.playerAnsweredCorrectly) {
                        delay(1000)
                        _playerVHunterBoardState.update { it?.copy(playerCorrectAnswers = it.playerCorrectAnswers + 1) }
                    }
                    if (event.dto.hunterAnsweredCorrectly) {
                        delay(1000)
                        _playerVHunterBoardState.update { it?.copy(hunterCorrectAnswers = it.hunterCorrectAnswers + 1) }
                    }
                }
            }

            is GameSessionSocketEvent.PlayerWonEvent -> {

                _gameSessionState.update {
                    it.copy(
                        gameSessionPlayers = event.dto.playersListUpdated
                    )
                }

                val username = event.dto.playersListUpdated[event.dto.playerWonId]!!.playerName

                viewModelScope.launch { _gameEvent.send(PlayerWon(username, event.dto.moneyWon)) }
            }

            is GameSessionSocketEvent.PlayerCaughtEvent -> {
                _gameSessionState.update {
                    it.copy(
                        gameSessionPlayers = event.dto.playersListUpdated
                    )
                }

                val username = event.dto.playersListUpdated[event.dto.playerCaughtId]!!.playerName
                viewModelScope.launch { _gameEvent.send(PlayerCaught(username)) }
            }

            is GameSessionSocketEvent.BoardPhaseFinishedEvent -> {
                viewModelScope.launch { _gameEvent.send(BoardPhaseFinished) }
            }

            is GameSessionSocketEvent.GameFinishedEvent -> {
                _gameResults.value = event.results
                _gameSessionState.update {
                    it.copy(
                        gamePhase = GamePhase.FINISHED
                    )
                }
            }

            is GameSessionSocketEvent.PlayersAnsweringPhaseStartEvent -> {
                _playersAnsweringPlayerList.update {
                    _gameSessionState.value.gameSessionPlayers.filter {
                        it.value.isHunter.not() && it.value.isEliminated.not()
                    }.map {
                        PlayersAnsweringPlayer(
                            playerId = it.value.playerId,
                            name = it.value.playerName,
                            emoji = phaseEmojis[it.value.playerId.toInt() % phaseEmojis.size]
                        )
                    }
                }

                _totalSteps.update { _playersAnsweringPlayerList.value.size }

                _questionText.update { event.dto.question.question }

                _gameSessionState.update {
                    it.copy(
                        gamePhase = GamePhase.PLAYERS_ANSWERING
                    )
                }
            }

            is GameSessionSocketEvent.PlayerBuzzedInEvent -> {
                _currentAnsweringPlayerId.value = event.dto.playerId
            }

            is GameSessionSocketEvent.PlayersAnsweringCorrectEvent -> {
                _playerAnsweredCorrectly.update { true }
                _currentAnsweringPlayerId.update { null }
                _correctAnswer.update { event.dto.correctAnswer }
                _coinBoosterCorrectAnswers.update { it + 1 }
                _totalSteps.update { it + 1 }
            }

            is GameSessionSocketEvent.PlayersAnsweringNextQuestionEvent -> {
                _playerAnsweredCorrectly.value = null
                _currentAnsweringPlayerId.update { null }
                _correctAnswer.update { null }
                _questionText.update { event.dto.question.question }
            }

            is GameSessionSocketEvent.PlayersAnsweringWrongEvent -> {
                _playerAnsweredCorrectly.update { false }
                _correctAnswer.update { event.dto.correctAnswer }
            }

            is GameSessionSocketEvent.PlayersAnsweringPhaseFinishedEvent -> {
                viewModelScope.launch {
                    _gameEvent.send(PlayersAnsweringFinished)
                }
            }

            is GameSessionSocketEvent.HunterAnsweringPhaseStartEvent -> {

                val dto = event.dto

                _hunterAnsweringPhaseState.update {
                    HunterAnsweringPhaseState(
                        playersSteps = dto.hunterAnsweringState.totalStepsToReach,
                        hunterCorrectAnswers = 0,
                        question = dto.question.question ?: "",
                        correctAnswer = null,
                        hunterIsAnswering = true,
                        playersAreAnswering = false,
                        players = _playersAnsweringPlayerList.value,
                        endTimestamp = dto.endTimestamp
                    )
                }
                _gameSessionState.update { it.copy(gamePhase = GamePhase.HUNTER_ANSWERING) }
            }

            is GameSessionSocketEvent.HunterAnsweredCorrectEvent -> {
                val dto = event.dto

                _hunterAnsweringPhaseState.update {
                    it.copy(
                        hunterCorrectAnswers = it.hunterCorrectAnswers + 1,
                        correctAnswer = dto.correctAnswer,
                        hunterAnsweredCorrectly = true
                    )
                }
            }

            is GameSessionSocketEvent.HunterAnsweredWrongEvent -> {
                _hunterAnsweringPhaseState.update {
                    it.copy(
                        hunterIsAnswering = false,
                        playersAreAnswering = true,
                        hunterAnsweredCorrectly = false
                    )
                }
            }

            is GameSessionSocketEvent.HunterAnsweringNextQuestionEvent -> {
                val dto = event.dto

                _hunterAnsweringPhaseState.update {
                    it.copy(
                        question = dto.question,
                        correctAnswer = null,
                        hunterAnsweredCorrectly = null,
                        playersAnsweredCorrectly = null,
                        hunterIsAnswering = true,
                        playersAreAnswering = false
                    )
                }

                _suggestions.update { emptyList() }
            }


            is GameSessionSocketEvent.PlayerCounterAnswerCorrectEvent -> {
                val dto = event.dto

                _hunterAnsweringPhaseState.update {

                    val newPlayersSteps = if (it.hunterCorrectAnswers == 0) {
                        it.playersSteps + 1
                    } else {
                        it.playersSteps
                    }

                    val newHunterCorrectAnswers = if (it.hunterCorrectAnswers == 0) {
                        0
                    } else {
                        it.hunterCorrectAnswers - 1
                    }

                    it.copy(
                        correctAnswer = dto.correctAnswer,
                        playersAnsweredCorrectly = true,
                        hunterAnsweredCorrectly = false,
                        playersSteps = newPlayersSteps,
                        hunterCorrectAnswers = newHunterCorrectAnswers
                    )

                }
            }

            is GameSessionSocketEvent.PlayerCounterAnswerWrongEvent -> {
                val dto = event.dto

                _hunterAnsweringPhaseState.update {
                    it.copy(
                        correctAnswer = dto.correctAnswer,
                        playersAnsweredCorrectly = false,
                        hunterAnsweredCorrectly = false
                    )
                }
            }

            is GameSessionSocketEvent.HunterAnsweringPhaseFinishedEvent -> {
                viewModelScope.launch {

                    _hunterAnsweringPhaseState.update {
                        it.copy(
                            hunterWon = event.dto.hunterWon
                        )
                    }

                    _gameEvent.send(GameEvent.HunterAnsweringFinished)
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

                _hunterAnsweringPhaseState.update {
                    it.copy(playersAreAnswering = true)
                }
            }

            is GameSessionSocketEvent.HunterTimerResumedEvent -> {

                val dto = event.dto

                _hunterAnsweringPhaseState.update {
                    it.copy(
                        playersAreAnswering = false,
                        endTimestamp = dto.endTimestamp
                    )
                }
            }

            is GameSessionSocketEvent.PlayerLeftEvent -> {}
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

sealed class GameEvent {
    data class PlayerWon(val username: String, val money: Float) : GameEvent()
    data class PlayerCaught(val username: String) : GameEvent()
    object BoardPhaseFinished : GameEvent()
    object PlayersAnsweringFinished : GameEvent()
    object HunterAnsweringFinished : GameEvent()
}

data class PlayersAnsweringPlayer(val playerId: Long, val name: String, val emoji: String)

data class HunterAnsweringPhaseState(
    val hunterWon: Boolean? = null,
    val playersSteps: Int = 0,
    val hunterCorrectAnswers: Int = 0,
    val question: String = "",
    val correctAnswer: String? = null,
    val hunterIsAnswering: Boolean = true,
    val playersAreAnswering: Boolean = false,
    val players: List<PlayersAnsweringPlayer> = emptyList(),
    val hunterAnsweredCorrectly: Boolean? = null, //null -> answer not given yet
    val playersAnsweredCorrectly: Boolean? = null,
    val endTimestamp: Long = 0
)
