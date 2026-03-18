package com.fran.dev.potjera.android.app.game.repository

import android.content.SharedPreferences
import android.util.Log
import com.fran.dev.potjera.android.app.game.AnswerMatcher
import com.fran.dev.potjera.android.app.game.models.BoardQuestion
import com.fran.dev.potjera.android.app.game.models.CoinBoosterQuestion
import com.fran.dev.potjera.android.app.game.models.GameSessionPlayer
import com.fran.dev.potjera.android.app.game.models.dto.CoinBoosterStartPlayerDto
import com.fran.dev.potjera.android.app.game.models.dto.board.AnswerRevealDto
import com.fran.dev.potjera.android.app.game.models.dto.board.BoardPhaseFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.board.BoardPhaseStartingDto
import com.fran.dev.potjera.android.app.game.models.dto.board.BoardQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.HunterAnsweredQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.MoneyOfferAcceptedDto
import com.fran.dev.potjera.android.app.game.models.dto.board.MoneyOfferDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerAnsweredQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerCaughtDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerVHunterBoardStateDto
import com.fran.dev.potjera.android.app.game.models.dto.board.PlayerWonDto
import com.fran.dev.potjera.android.app.game.models.dto.coinbooster.CoinBoosterFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweredCorrectDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweredWrongDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringNextQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringPhaseFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringPhaseStartDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringPhaseSuggestionDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterAnsweringStateDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterTimerPausedDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.HunterTimerResumedDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.PlayerCounterAnswerCorrectDto
import com.fran.dev.potjera.android.app.game.models.dto.hunterphase.PlayerCounterAnswerWrongDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayerSignedInDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringCorrectDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringFinishedDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringNextQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringQuestionDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringStartDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringStateDto
import com.fran.dev.potjera.android.app.game.models.dto.playersansweringphase.PlayersAnsweringWrongDto
import com.fran.dev.potjera.android.app.game.models.enums.BoardPhase
import com.fran.dev.potjera.android.app.game.models.event.GameSessionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD }

data class DifficultyConfig(
    val hunterCorrectProbability: Float,
    val botBoardWinProbability: Float,
    val botAnswerCorrectProbability: Float,
    val botBuzzDelayRange: LongRange,
)

@Singleton
class SingleplayerGameSessionRepository @Inject constructor(
    private val quizRepository: QuizRepository,
    prefs: SharedPreferences,
) : GameSessionRepository {

    private val gameScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "SingleplayerRepo"

        const val HUNTER_ID = 100L
        const val BOT_ID_START = 10L
        private val BOT_NAMES = listOf("Alex", "Jamie", "Morgan")
        private const val BOT_COUNT = 3
        private const val PHASE_TIMEOUT_MS = 2 * 60 * 1_000L
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    val myPlayerId: Long = prefs.getLong("user_id", 0L)
    val myPlayerName: String = prefs.getString("username", "You") ?: "You"

    // ── Events ────────────────────────────────────────────────────────────────

    private val _events = MutableSharedFlow<GameSessionEvent>()
    override val events: SharedFlow<GameSessionEvent> = _events.asSharedFlow()

    // ── Difficulty ────────────────────────────────────────────────────────────

    private val difficultyConfigs = mapOf(
        Difficulty.EASY to DifficultyConfig(0.40f, 0.30f, 0.30f, 6_000L..12_000L),
        Difficulty.MEDIUM to DifficultyConfig(0.65f, 0.55f, 0.60f, 3_000L..7_000L),
        Difficulty.HARD to DifficultyConfig(0.85f, 0.80f, 0.85f, 1_000L..3_500L),
    )

    lateinit var difficultyConfig: DifficultyConfig

    // ── Question pools ────────────────────────────────────────────────────────

    private val _quickFireQuestions = MutableStateFlow<List<CoinBoosterQuestion>>(emptyList())
    private val _multipleChoiceQuestions = MutableStateFlow<List<BoardQuestion>>(emptyList())

    // ── Shared game state ─────────────────────────────────────────────────────

    private var playersInfo = emptyMap<Long, GameSessionPlayer>()
    private val botCoinBoosterEarnings = mutableMapOf<Long, Float>()

    private val survivingBots: List<GameSessionPlayer>
        get() = playersInfo.values.filter { !it.isHunter && !it.isEliminated && it.playerId != myPlayerId }

    // ── Interface: connect / disconnect ──────────────────────────────────────

    override suspend fun connect(difficulty: Difficulty?) {
        if (difficulty == null) return
        Log.d(TAG, "connect: starting singleplayer with difficulty=$difficulty")
        start(difficulty)
    }

    override fun connect(gameSessionId: String, token: String) = Unit
    override fun disconnect() = Unit

    // ─────────────────────────────────────────────────────────────────────────
    // GAME BOOTSTRAP
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun start(difficulty: Difficulty) {
        Log.i(TAG, "start: difficulty=$difficulty player=$myPlayerName id=$myPlayerId")
        difficultyConfig = difficultyConfigs[difficulty] ?: difficultyConfigs[Difficulty.EASY]!!
        fetchQuestions()
        startGame()
    }

    private suspend fun fetchQuestions() {
        Log.d(TAG, "fetchQuestions: fetching question pools")
        quizRepository.getQuickFireQuestions(30)
            .onSuccess {
                _quickFireQuestions.value = it
                Log.d(TAG, "fetchQuestions: loaded ${it.size} quick-fire questions")
            }
            .onFailure { Log.e(TAG, "fetchQuestions: failed to load quick-fire questions", it) }

        quizRepository.getMultipleChoiceQuestions(15)
            .onSuccess {
                _multipleChoiceQuestions.value = it
                Log.d(TAG, "fetchQuestions: loaded ${it.size} multiple-choice questions")
            }
            .onFailure {
                Log.e(
                    TAG,
                    "fetchQuestions: failed to load multiple-choice questions",
                    it
                )
            }
    }

    private suspend fun startGame() {
        val human = buildHumanPlayer()
        val bots = buildBotPlayers()
        val hunter = buildHunter()

        playersInfo = (bots + human + hunter).associateBy { it.playerId }
        Log.i(TAG, "startGame: roster built — players=${playersInfo.values.map { it.playerName }}")

        _events.emit(
            GameSessionEvent.CoinBoosterStartedPlayerEvent(
                dto = CoinBoosterStartPlayerDto(
                    playersInfo = playersInfo,
                    questions = _quickFireQuestions.value.take(15)
                )
            )
        )
        Log.d(TAG, "startGame: CoinBoosterStartedPlayerEvent emitted")

        playBotsCoinBooster(bots)
    }

    private fun buildHumanPlayer() = GameSessionPlayer(
        playerId = myPlayerId, playerName = myPlayerName,
        moneyWon = 0f, isEliminated = false, isCaptain = true,
        isHunter = false, isHost = true, hasPlayedBoard = false
    )

    private fun buildBotPlayers() = (0 until BOT_COUNT).map { i ->
        GameSessionPlayer(
            playerId = BOT_ID_START + i, playerName = BOT_NAMES[i],
            moneyWon = 0f, isEliminated = false, isCaptain = false,
            isHunter = false, isHost = false, hasPlayedBoard = false
        )
    }

    private fun buildHunter() = GameSessionPlayer(
        playerId = HUNTER_ID, playerName = "Hunter",
        moneyWon = 0f, isEliminated = false, isCaptain = false,
        isHunter = true, isHost = false, hasPlayedBoard = false
    )

    // ─────────────────────────────────────────────────────────────────────────
    // COIN BOOSTER PHASE
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun playBotsCoinBooster(bots: List<GameSessionPlayer>) {
        Log.d(TAG, "playBotsCoinBooster: simulating ${bots.size} bots")
        bots.forEach { bot ->
            val moneyWon = Random.nextInt(2, 12) * 500f
            botCoinBoosterEarnings[bot.playerId] = moneyWon
            Log.d(TAG, "playBotsCoinBooster: ${bot.playerName} earned \$$moneyWon")
            _events.emit(
                GameSessionEvent.CoinBoosterFinishedEvent(
                    payload = CoinBoosterFinishedDto(
                        playerId = bot.playerId,
                        username = bot.playerName,
                        moneyWon = moneyWon
                    )
                )
            )
        }
    }

    override suspend fun finishCoinBooster(correctAnswers: Int) {
        val moneyWon = correctAnswers * 500f
        Log.i(TAG, "finishCoinBooster: correctAnswers=$correctAnswers moneyWon=\$$moneyWon")
        _events.emit(
            GameSessionEvent.CoinBoosterFinishedEvent(
                payload = CoinBoosterFinishedDto(
                    playerId = myPlayerId,
                    username = myPlayerName,
                    moneyWon = moneyWon
                )
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOARD PHASE
    // ─────────────────────────────────────────────────────────────────────────

    var playerVHunterBoardState: PlayerVHunterBoardStateDto = PlayerVHunterBoardStateDto(
        questionsStarted = false,
        boardQuestion = null,
        hunterAnswer = null,
        playerAnswer = null,
        hunterCorrectAnswers = 0,
        playerCorrectAnswers = 0,
        playerStartingIndex = 2,
        moneyInGame = 0f,
        boardPhase = BoardPhase.HUNTER_MAKING_OFFER.name
    )
    var playerVHunterCurrentQuestionIndex = 0

    private var boardHunterAnswerJob: Job? = null

    override suspend fun startBoardPhase(moneyWon: Float) {
        Log.i(TAG, "startBoardPhase: moneyInPlay=\$$moneyWon")

        playerVHunterBoardState = PlayerVHunterBoardStateDto(
            questionsStarted = false, boardQuestion = null,
            hunterAnswer = null, playerAnswer = null,
            hunterCorrectAnswers = 0, playerCorrectAnswers = 0,
            playerStartingIndex = 2, moneyInGame = moneyWon,
            boardPhase = BoardPhase.HUNTER_MAKING_OFFER.name
        )

        _events.emit(
            GameSessionEvent.BoardPhaseStartingEvent(
                dto = BoardPhaseStartingDto(
                    currentPlayerId = myPlayerId,
                    boardState = playerVHunterBoardState
                )
            )
        )

        delay(2_000)

        val higherOffer = (moneyWon * (Random.nextFloat() * 0.40f + 1.20f)).roundToNearest100()
        val lowerOffer = (moneyWon * (Random.nextFloat() * 0.30f + 0.40f)).roundToNearest100()
        Log.d(TAG, "startBoardPhase: generated offer higher=\$$higherOffer lower=\$$lowerOffer")

        sendMoneyOffer(higherOffer, lowerOffer)
    }

    override suspend fun sendMoneyOffer(higherOffer: Float, lowerOffer: Float) {
        Log.d(TAG, "sendMoneyOffer: higher=\$$higherOffer lower=\$$lowerOffer")
        _events.emit(GameSessionEvent.MoneyOfferEvent(dto = MoneyOfferDto(higherOffer, lowerOffer)))
    }

    override suspend fun sendMoneyOfferResponse(acceptedOffer: Float) {
        val playerStartingIndex = when {
            acceptedOffer > playerVHunterBoardState.moneyInGame -> 1
            acceptedOffer < playerVHunterBoardState.moneyInGame -> 3
            else -> 2
        }
        Log.i(
            TAG,
            "sendMoneyOfferResponse: accepted=\$$acceptedOffer startingIndex=$playerStartingIndex"
        )

        playerVHunterBoardState = playerVHunterBoardState.copy(
            moneyInGame = acceptedOffer,
            playerStartingIndex = playerStartingIndex
        )

        _events.emit(
            GameSessionEvent.MoneyOfferAcceptedEvent(
                dto = MoneyOfferAcceptedDto(
                    playerStartingIndex = playerStartingIndex,
                    moneyInGame = acceptedOffer
                )
            )
        )

        delay(3_000)
        sendStartBoardQuestions()
    }

    suspend fun sendStartBoardQuestions() {
        Log.d(TAG, "sendStartBoardQuestions: starting first board question")

        startBoardSession()
    }

    private suspend fun startBoardSession() {
        val q = _multipleChoiceQuestions.value[playerVHunterCurrentQuestionIndex]
        Log.d(
            TAG,
            "startBoardSession: question[${playerVHunterCurrentQuestionIndex}] \"${q.question}\""
        )

        val boardQuestionDto = q.toDto()

        playerVHunterBoardState = playerVHunterBoardState.copy(
            questionsStarted = true,
            boardQuestion = boardQuestionDto,
            playerAnswer = null,
            hunterAnswer = null,
            boardPhase = BoardPhase.QUESTION_READING.name
        )

        _events.emit(GameSessionEvent.NewBoardQuestionEvent(dto = boardQuestionDto))
        scheduleBoardHunterAnswer()
    }

    private fun scheduleBoardHunterAnswer() {
        val delay = Random.nextLong(
            difficultyConfig.botBuzzDelayRange.first,
            difficultyConfig.botBuzzDelayRange.last
        )
        Log.d(TAG, "scheduleBoardHunterAnswer: hunter will answer in ${delay}ms")
        boardHunterAnswerJob = gameScope.launch {
            delay(delay)
            emitBoardHunterAnswer()
        }
    }

    private suspend fun emitBoardHunterAnswer() {
        val question = playerVHunterBoardState.boardQuestion ?: return
        val answer = generateBoardHunterAnswer(question)
        Log.d(
            TAG,
            "emitBoardHunterAnswer: hunter answered \"$answer\" (correct=\"${question.correctAnswer}\")"
        )

        playerVHunterBoardState = playerVHunterBoardState.copy(
            hunterAnswer = answer,
            boardPhase = BoardPhase.ANSWER_GIVEN.name
        )
        _events.emit(
            GameSessionEvent.HunterAnsweredQuestionEvent(
                dto = HunterAnsweredQuestionDto(
                    answer
                )
            )
        )

        if (playerVHunterBoardState.playerAnswer != null) {
            Log.d(TAG, "emitBoardHunterAnswer: player already answered — triggering reveal")
            delay(2_000)
            revealBoardAnswer()
        }
    }

    private fun generateBoardHunterAnswer(question: BoardQuestionDto): String {
        val correct = Random.nextFloat() < difficultyConfig.hunterCorrectProbability
        return if (correct) question.correctAnswer
        else question.choices.filter { it != question.correctAnswer }.randomOrNull()
            ?: question.correctAnswer
    }

    override suspend fun sendBoardAnswer(answer: String, isHunter: Boolean) {
        val playerWasFaster = boardHunterAnswerJob?.isActive == true
        boardHunterAnswerJob?.cancel()
        boardHunterAnswerJob = null

        Log.i(TAG, "sendBoardAnswer: answer=\"$answer\" playerWasFaster=$playerWasFaster")

        playerVHunterBoardState = playerVHunterBoardState.copy(
            playerAnswer = answer,
            boardPhase = BoardPhase.ANSWER_GIVEN.name
        )
        _events.emit(
            GameSessionEvent.PlayerAnsweredQuestionEvent(
                dto = PlayerAnsweredQuestionDto(
                    answer
                )
            )
        )

        if (playerWasFaster) {
            val hunterDelay = Random.nextLong(1_000L, 2_000L)
            Log.d(TAG, "sendBoardAnswer: hunter will answer in ${hunterDelay}ms")
            delay(hunterDelay)
            emitBoardHunterAnswer()
        } else {
            delay(2_000)
            revealBoardAnswer()
        }
    }

    private suspend fun revealBoardAnswer() {
        val question = playerVHunterBoardState.boardQuestion ?: return
        val playerCorrect = playerVHunterBoardState.playerAnswer == question.correctAnswer
        val hunterCorrect = playerVHunterBoardState.hunterAnswer == question.correctAnswer

        Log.i(
            TAG, "revealBoardAnswer: playerCorrect=$playerCorrect hunterCorrect=$hunterCorrect" +
                    " | player=\"${playerVHunterBoardState.playerAnswer}\" hunter=\"${playerVHunterBoardState.hunterAnswer}\"" +
                    " | correct=\"${question.correctAnswer}\""
        )

        playerVHunterBoardState = playerVHunterBoardState.copy(
            playerCorrectAnswers = playerVHunterBoardState.playerCorrectAnswers + if (playerCorrect) 1 else 0,
            hunterCorrectAnswers = playerVHunterBoardState.hunterCorrectAnswers + if (hunterCorrect) 1 else 0
        )

        _events.emit(
            GameSessionEvent.AnswerRevealedEvent(
                dto = AnswerRevealDto(
                    playerAnsweredCorrectly = playerCorrect,
                    hunterAnsweredCorrectly = hunterCorrect
                )
            )
        )

        delay(2_500)
        advanceBoardPhase()
    }

    private suspend fun advanceBoardPhase() {
        val playerPos = playerVHunterBoardState.playerStartingIndex +
                playerVHunterBoardState.playerCorrectAnswers
        val hunterPos = playerVHunterBoardState.hunterCorrectAnswers - 1

        Log.d(
            TAG, "advanceBoardPhase: playerPos=$playerPos hunterPos=$hunterPos" +
                    " (playerCorrect=${playerVHunterBoardState.playerCorrectAnswers}" +
                    " hunterCorrect=${playerVHunterBoardState.hunterCorrectAnswers})"
        )

        when {
            playerPos >= 7 -> onPlayerEscaped()
            hunterPos >= playerPos -> onPlayerCaught()
            else -> proceedToNextBoardQuestion()
        }
    }

    private suspend fun onPlayerEscaped() {
        Log.i(TAG, "onPlayerEscaped: player won \$${playerVHunterBoardState.moneyInGame}")
        playersInfo = playersInfo.updatePlayer(myPlayerId) {
            it.copy(moneyWon = playerVHunterBoardState.moneyInGame, hasPlayedBoard = true)
        }
        _events.emit(
            GameSessionEvent.PlayerWonEvent(
                dto = PlayerWonDto(
                    playerWonId = myPlayerId,
                    moneyWon = playerVHunterBoardState.moneyInGame,
                    playersListUpdated = playersInfo
                )
            )
        )
        delay(2_000)
        finishBoardSession()
    }

    private suspend fun onPlayerCaught() {
        Log.i(TAG, "onPlayerCaught: player was caught by hunter")
        playersInfo = playersInfo.updatePlayer(myPlayerId) {
            it.copy(isEliminated = true, moneyWon = 0f, hasPlayedBoard = true)
        }
        _events.emit(
            GameSessionEvent.PlayerCaughtEvent(
                dto = PlayerCaughtDto(
                    playerCaughtId = myPlayerId,
                    playersListUpdated = playersInfo
                )
            )
        )
        delay(2_000)
        finishBoardSession()
    }

    private suspend fun proceedToNextBoardQuestion() {
        playerVHunterCurrentQuestionIndex++
        Log.d(
            TAG,
            "proceedToNextBoardQuestion: index=$playerVHunterCurrentQuestionIndex / ${_multipleChoiceQuestions.value.size}"
        )

        if (playerVHunterCurrentQuestionIndex >= _multipleChoiceQuestions.value.size) {
            Log.d(TAG, "proceedToNextBoardQuestion: exhausted batch — fetching more")
            quizRepository.getMultipleChoiceQuestions(15)
                .onSuccess { newQuestions ->
                    Log.d(
                        TAG,
                        "proceedToNextBoardQuestion: fetched ${newQuestions.size} new questions"
                    )
                    _multipleChoiceQuestions.value = newQuestions
                    playerVHunterCurrentQuestionIndex = 0
                    startBoardSession()
                }
                .onFailure {
                    Log.e(
                        TAG,
                        "proceedToNextBoardQuestion: fetch failed — finishing board session",
                        it
                    )
                    finishBoardSession()
                }
        } else {
            startBoardSession()
        }
    }

    private suspend fun finishBoardSession() {
        Log.i(
            TAG,
            "finishBoardSession: simulating ${survivingBots.size + playersInfo.values.count { !it.isHunter && it.playerId != myPlayerId }} bot board results"
        )

        playersInfo.values
            .filter { !it.isHunter && it.playerId != myPlayerId }
            .forEach { bot ->
                val botWon = Random.nextFloat() < difficultyConfig.botBoardWinProbability
                val botMoney = if (botWon) simulateBotBoardEarnings(bot.playerId) else 0f
                Log.d(TAG, "finishBoardSession: ${bot.playerName} — won=$botWon money=\$$botMoney")
                playersInfo = playersInfo.updatePlayer(bot.playerId) {
                    it.copy(moneyWon = botMoney, isEliminated = !botWon, hasPlayedBoard = true)
                }
            }

        Log.i(
            TAG,
            "finishBoardSession: survivors=${
                playersInfo.values.filter { !it.isHunter && !it.isEliminated }.map { it.playerName }
            }"
        )
        _events.emit(
            GameSessionEvent.BoardPhaseFinishedEvent(
                dto = BoardPhaseFinishedDto(players = playersInfo)
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PLAYERS ANSWERING PHASE
    // ─────────────────────────────────────────────────────────────────────────

    private val _playersAnsweringQuestions =
        MutableStateFlow<List<CoinBoosterQuestion>>(emptyList())
    private var playersAnsweringCorrectAnswers = 0
    private var playersAnsweringQuestionIndex = 0
    private var playersAnsweringSignedPlayerId: Long? = null

    private var botBuzzJob: Job? = null
    private var phaseTimeoutJob: Job? = null

    override suspend fun startPlayersAnsweringPhase() {
        Log.i(
            TAG,
            "startPlayersAnsweringPhase: survivingBots=${survivingBots.map { it.playerName }}"
        )

        quizRepository.getQuickFireQuestions(15)
            .onFailure {
                Log.e(
                    TAG,
                    "startPlayersAnsweringPhase: failed to fetch questions",
                    it
                ); return
            }
            .onSuccess { _playersAnsweringQuestions.value = it }

        playersAnsweringCorrectAnswers = 0
        playersAnsweringQuestionIndex = 0
        playersAnsweringSignedPlayerId = null

        _events.emit(
            GameSessionEvent.PlayersAnsweringPhaseStartEvent(
                dto = PlayersAnsweringStartDto(
                    playersAnsweringState = PlayersAnsweringStateDto(
                        correctAnswers = 0,
                        signedPlayerId = null,
                        currentQuestionIndex = 0
                    ),
                    question = _playersAnsweringQuestions.value.first().toPlayersAnsweringDto(),
                    questionNum = 1
                )
            )
        )

        phaseTimeoutJob = gameScope.launch {
            Log.d(TAG, "startPlayersAnsweringPhase: ${PHASE_TIMEOUT_MS / 1000}s timeout started")
            delay(PHASE_TIMEOUT_MS)
            Log.i(TAG, "startPlayersAnsweringPhase: timeout expired")
            finishPlayersAnsweringPhase()
        }

        scheduleBotBuzz()
    }

    override suspend fun buzzIn() {
        if (playersAnsweringSignedPlayerId != null) {
            Log.d(TAG, "buzzIn: ignored — ${playersAnsweringSignedPlayerId} already signed in")
            return
        }
        Log.d(TAG, "buzzIn: player $myPlayerId buzzed in")
        playersAnsweringSignedPlayerId = myPlayerId
        botBuzzJob?.cancel()
        _events.emit(GameSessionEvent.PlayerBuzzedInEvent(dto = PlayerSignedInDto(myPlayerId)))
    }

    override suspend fun sendPlayersAnsweringAnswer(answer: String) {
        val question =
            _playersAnsweringQuestions.value.getOrNull(playersAnsweringQuestionIndex) ?: return
        val isSolo = survivingBots.isEmpty()
        val isSignedIn = playersAnsweringSignedPlayerId == myPlayerId
        if (!isSolo && !isSignedIn) {
            Log.w(TAG, "sendPlayersAnsweringAnswer: rejected — player is not signed in")
            return
        }

        val isCorrect = AnswerMatcher.isCorrect(answer, question.answer, question.aliases)
        Log.i(
            TAG,
            "sendPlayersAnsweringAnswer: answer=\"$answer\" correct=$isCorrect (expected=\"${question.answer}\")"
        )
        emitPlayersAnsweringResult(myPlayerId, isCorrect, question.answer)
    }

    private suspend fun emitPlayersAnsweringResult(
        answererId: Long,
        isCorrect: Boolean,
        correctAnswer: String,
    ) {
        playersAnsweringSignedPlayerId = null

        if (isCorrect) {
            playersAnsweringCorrectAnswers++
            Log.d(
                TAG,
                "emitPlayersAnsweringResult: correct — total=$playersAnsweringCorrectAnswers"
            )
            _events.emit(
                GameSessionEvent.PlayersAnsweringCorrectEvent(
                    dto = PlayersAnsweringCorrectDto(
                        playerId = answererId,
                        correctAnswer = correctAnswer
                    )
                )
            )
        } else {
            Log.d(TAG, "emitPlayersAnsweringResult: wrong — correct was \"$correctAnswer\"")
            _events.emit(
                GameSessionEvent.PlayersAnsweringWrongEvent(
                    dto = PlayersAnsweringWrongDto(
                        playerId = answererId,
                        correctAnswer = correctAnswer
                    )
                )
            )
        }

        delay(1_500)
        sendNextPlayersAnsweringQuestion()
    }

    private fun scheduleBotBuzz() {
        botBuzzJob?.cancel()
        if (survivingBots.isEmpty()) return

        val totalDelay = 5_000L + Random.nextLong(
            difficultyConfig.botBuzzDelayRange.first,
            difficultyConfig.botBuzzDelayRange.last
        )
        val bot = survivingBots.random()
        Log.d(TAG, "scheduleBotBuzz: ${bot.playerName} will buzz in ${totalDelay}ms")

        botBuzzJob = gameScope.launch {
            delay(totalDelay)
            if (playersAnsweringSignedPlayerId != null) {
                Log.d(TAG, "scheduleBotBuzz: ${bot.playerName} buzz cancelled — already signed in")
                return@launch
            }

            playersAnsweringSignedPlayerId = bot.playerId
            Log.d(TAG, "scheduleBotBuzz: ${bot.playerName} buzzed in")
            _events.emit(GameSessionEvent.PlayerBuzzedInEvent(dto = PlayerSignedInDto(bot.playerId)))

            delay(Random.nextLong(800L, 2_000L))

            val question = _playersAnsweringQuestions.value
                .getOrNull(playersAnsweringQuestionIndex) ?: return@launch

            val isCorrect = Random.nextFloat() < (1f - difficultyConfig.botAnswerCorrectProbability)
            Log.d(TAG, "scheduleBotBuzz: ${bot.playerName} answered — correct=$isCorrect")
            emitPlayersAnsweringResult(bot.playerId, isCorrect, question.answer)
        }
    }

    private suspend fun sendNextPlayersAnsweringQuestion() {
        val nextIndex = playersAnsweringQuestionIndex + 1

        if (nextIndex >= _playersAnsweringQuestions.value.size) {
            Log.d(TAG, "sendNextPlayersAnsweringQuestion: batch exhausted — fetching more")
            quizRepository.getQuickFireQuestions(15)
                .onFailure {
                    Log.e(TAG, "sendNextPlayersAnsweringQuestion: fetch failed", it)
                    finishPlayersAnsweringPhase()
                    return
                }
                .onSuccess {
                    Log.d(
                        TAG,
                        "sendNextPlayersAnsweringQuestion: fetched ${it.size} more questions"
                    )
                    _playersAnsweringQuestions.value += it
                }
        }

        playersAnsweringQuestionIndex = nextIndex
        playersAnsweringSignedPlayerId = null
        Log.d(TAG, "sendNextPlayersAnsweringQuestion: question[$nextIndex]")

        _events.emit(
            GameSessionEvent.PlayersAnsweringNextQuestionEvent(
                dto = PlayersAnsweringNextQuestionDto(
                    question = _playersAnsweringQuestions.value[nextIndex].toPlayersAnsweringDto(),
                    questionNum = nextIndex + 1,
                    total = _playersAnsweringQuestions.value.size
                )
            )
        )

        scheduleBotBuzz()
    }

    private suspend fun finishPlayersAnsweringPhase() {
        // Cancel jobs first — this stops any in-flight botBuzzJob from emitting
        // more NextQuestion events after the Finished event
        //phaseTimeoutJob?.cancel()
        botBuzzJob?.cancel()
        //phaseTimeoutJob = null
        botBuzzJob = null

        Log.i(TAG, "finishPlayersAnsweringPhase: correctAnswers=$playersAnsweringCorrectAnswers")

        _events.emit(
            GameSessionEvent.PlayersAnsweringPhaseFinishedEvent(
                dto = PlayersAnsweringFinishedDto(
                    correctAnswers = playersAnsweringCorrectAnswers,
                    playerIds = playersInfo.values
                        .filter { !it.isHunter && !it.isEliminated }
                        .map { it.playerId }
                )
            )
        )


        Log.i(TAG, "finishPlayersAnsweringPhase: event: PlayersAnsweringPhaseFinishedEvent emitted")

        delay(3_000)
        startHunterAnsweringPhase()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUNTER ANSWERING PHASE
    // ─────────────────────────────────────────────────────────────────────────

    private val _hunterAnsweringQuestions = MutableStateFlow<List<CoinBoosterQuestion>>(emptyList())
    private var hunterCorrectAnswers = 0
    private var hunterTotalStepsToReach = 0
    private var hunterQuestionIndex = 0
    private var hunterJustWrong = false

    private var hunterTimerJob: Job? = null
    private var hunterAnswerJob: Job? = null
    private var suggestionJob: Job? = null

    private var hunterTimerEndMs: Long = 0L
    private var hunterRemainingMs: Long = 0L

    private suspend fun startHunterAnsweringPhase() {
        quizRepository.getQuickFireQuestions(15)
            .onFailure {
                Log.e(
                    TAG,
                    "startHunterAnsweringPhase: failed to fetch questions",
                    it
                ); return
            }
            .onSuccess { _hunterAnsweringQuestions.value = it }

        hunterCorrectAnswers = 0
        hunterQuestionIndex = 0
        hunterJustWrong = false

        val alivePlayers = playersInfo.values.count { !it.isHunter && !it.isEliminated }
        hunterTotalStepsToReach = alivePlayers + playersAnsweringCorrectAnswers

        Log.i(
            TAG, "startHunterAnsweringPhase: totalSteps=$hunterTotalStepsToReach" +
                    " (alivePlayers=$alivePlayers + playersCorrect=$playersAnsweringCorrectAnswers)"
        )

        val durationMs = PHASE_TIMEOUT_MS
        hunterTimerEndMs = System.currentTimeMillis() + durationMs

        _events.emit(
            GameSessionEvent.HunterAnsweringPhaseStartEvent(
                dto = HunterAnsweringPhaseStartDto(
                    hunterAnsweringState = HunterAnsweringStateDto(
                        hunterCorrectAnswers = 0,
                        totalStepsToReach = hunterTotalStepsToReach,
                        currentQuestionIndex = 0,
                        hunterJustWrong = false
                    ),
                    question = _hunterAnsweringQuestions.value.first().toHunterAnsweringDto(),
                    endTimestamp = hunterTimerEndMs
                )
            )
        )

        scheduleHunterTimer(durationMs)
        scheduleHunterAnswer()
    }

    private fun scheduleHunterTimer(durationMs: Long) {
        hunterTimerJob?.cancel()
        Log.d(TAG, "scheduleHunterTimer: timer set for ${durationMs / 1000}s")
        hunterTimerJob = gameScope.launch {
            delay(durationMs)
            Log.i(TAG, "scheduleHunterTimer: timer expired — hunter lost")
            finishHunterAnsweringPhase(hunterWon = false)
        }
    }

    private suspend fun pauseHunterTimer() {
        hunterRemainingMs = (hunterTimerEndMs - System.currentTimeMillis()).coerceAtLeast(0)
        hunterTimerJob?.cancel()
        Log.d(TAG, "pauseHunterTimer: remainingMs=$hunterRemainingMs")
        _events.emit(
            GameSessionEvent.HunterTimerPausedEvent(
                dto = HunterTimerPausedDto(
                    hunterRemainingMs
                )
            )
        )
    }

    private suspend fun resumeHunterTimer() {
        hunterTimerEndMs = System.currentTimeMillis() + hunterRemainingMs
        scheduleHunterTimer(hunterRemainingMs)
        Log.d(TAG, "resumeHunterTimer: resuming with ${hunterRemainingMs}ms remaining")
        _events.emit(
            GameSessionEvent.HunterTimerResumedEvent(
                dto = HunterTimerResumedDto(
                    hunterTimerEndMs
                )
            )
        )
    }

    private fun scheduleHunterAnswer() {
        hunterAnswerJob?.cancel()
        val delay = Random.nextLong(
            difficultyConfig.botBuzzDelayRange.first,
            difficultyConfig.botBuzzDelayRange.last
        )
        Log.d(TAG, "scheduleHunterAnswer: AI hunter will answer in ${delay}ms")
        hunterAnswerJob = gameScope.launch {
            delay(delay)
            if (!hunterJustWrong) aiHunterAnswer()
        }
    }

    override suspend fun sendHunterAnsweringAnswer(answer: String) {
        // Not implemented — AI hunter handles answering
    }

    private suspend fun aiHunterAnswer() {
        if (hunterJustWrong) return
        val question = _hunterAnsweringQuestions.value.getOrNull(hunterQuestionIndex) ?: return
        val isCorrect = Random.nextFloat() < difficultyConfig.hunterCorrectProbability

        Log.i(
            TAG,
            "aiHunterAnswer: question[${hunterQuestionIndex}] \"${question.question}\" — correct=$isCorrect" +
                    " (${hunterCorrectAnswers}/${hunterTotalStepsToReach})"
        )

        if (isCorrect) {
            hunterCorrectAnswers++
            _events.emit(
                GameSessionEvent.HunterAnsweredCorrectEvent(
                    dto = HunterAnsweredCorrectDto(
                        question.answer
                    )
                )
            )

            if (hunterCorrectAnswers >= hunterTotalStepsToReach) {
                Log.i(TAG, "aiHunterAnswer: hunter reached goal — hunter wins!")
                hunterTimerJob?.cancel()
                delay(1_500)
                finishHunterAnsweringPhase(hunterWon = true)
            } else {
                delay(1_500)
                sendNextHunterQuestion()
            }
        } else {
            Log.d(TAG, "aiHunterAnswer: hunter wrong — player counter-answer window open")
            hunterJustWrong = true

            val wrongAnswer = question.aliases.randomOrNull()
                ?: _quickFireQuestions.value
                    .filter { it.answer != question.answer }
                    .randomOrNull()?.answer
                ?: "?"

            _events.emit(
                GameSessionEvent.HunterAnsweredWrongEvent(
                    dto = HunterAnsweredWrongDto(
                        correctAnswer = question.answer,
                        hunterAnswer = wrongAnswer
                    )
                )
            )
            pauseHunterTimer()
            scheduleBotSuggestions(question)
        }
    }

    override suspend fun sendPlayerCounterAnswer(answer: String) {
        if (!hunterJustWrong) {
            Log.w(TAG, "sendPlayerCounterAnswer: rejected — not in counter-answer window")
            return
        }
        suggestionJob?.cancel()

        val question = _hunterAnsweringQuestions.value.getOrNull(hunterQuestionIndex) ?: return
        val isCorrect = AnswerMatcher.isCorrect(answer, question.answer, question.aliases)

        Log.i(
            TAG, "sendPlayerCounterAnswer: answer=\"$answer\" correct=$isCorrect" +
                    " | hunterCorrect=$hunterCorrectAnswers totalSteps=$hunterTotalStepsToReach"
        )

        hunterJustWrong = false

        if (isCorrect) {
            if (hunterCorrectAnswers == 0) {
                hunterTotalStepsToReach++
                Log.d(
                    TAG,
                    "sendPlayerCounterAnswer: hunter at 0 — steps pushed to $hunterTotalStepsToReach"
                )
            } else {
                hunterCorrectAnswers--
                Log.d(TAG, "sendPlayerCounterAnswer: hunter steps back to $hunterCorrectAnswers")
            }
            _events.emit(
                GameSessionEvent.PlayerCounterAnswerCorrectEvent(
                    dto = PlayerCounterAnswerCorrectDto(
                        question.answer
                    )
                )
            )
        } else {
            Log.d(TAG, "sendPlayerCounterAnswer: wrong — correct was \"${question.answer}\"")
            _events.emit(
                GameSessionEvent.PlayerCounterAnswerWrongEvent(
                    dto = PlayerCounterAnswerWrongDto(
                        question.answer
                    )
                )
            )
        }

        delay(1_500)
        resumeHunterTimer()
        sendNextHunterQuestion()
    }

    override suspend fun sendSuggestion(suggestion: String) { /* no-op — player is the captain in singleplayer */
    }

    private fun scheduleBotSuggestions(question: CoinBoosterQuestion) {
        suggestionJob?.cancel()
        if (survivingBots.isEmpty()) return

        Log.d(
            TAG,
            "scheduleBotSuggestions: ${survivingBots.size} bots may suggest for \"${question.question}\""
        )

        suggestionJob = gameScope.launch {
            survivingBots.forEach { bot ->
                val willSuggest =
                    Random.nextFloat() > difficultyConfig.hunterCorrectProbability * 0.5f
                if (!willSuggest) {
                    Log.d(TAG, "scheduleBotSuggestions: ${bot.playerName} decided not to suggest")
                    return@forEach
                }

                val suggestionDelay = Random.nextLong(5_000L, 12_000L)
                delay(suggestionDelay)
                if (!hunterJustWrong) {
                    Log.d(TAG, "scheduleBotSuggestions: ${bot.playerName} suggestion window closed")
                    return@forEach
                }

                val suggestsCorrectly =
                    Random.nextFloat() < (1f - difficultyConfig.hunterCorrectProbability * 0.6f)
                val suggestion = if (suggestsCorrectly) question.answer
                else _quickFireQuestions.value
                    .filter { it.answer != question.answer }
                    .randomOrNull()?.answer ?: question.answer

                Log.d(
                    TAG,
                    "scheduleBotSuggestions: ${bot.playerName} suggests \"$suggestion\" (correct=${suggestion == question.answer})"
                )

                _events.emit(
                    GameSessionEvent.HunterAnsweringSuggestionEvent(
                        dto = HunterAnsweringPhaseSuggestionDto(
                            sentBy = bot.playerId,
                            username = bot.playerName,
                            suggestion = suggestion
                        )
                    )
                )
            }
        }
    }

    private suspend fun sendNextHunterQuestion() {
        val nextIndex = hunterQuestionIndex + 1

        if (nextIndex >= _hunterAnsweringQuestions.value.size) {
            Log.d(TAG, "sendNextHunterQuestion: batch exhausted — fetching more")
            quizRepository.getQuickFireQuestions(15)
                .onFailure {
                    Log.e(TAG, "sendNextHunterQuestion: fetch failed", it)
                    finishHunterAnsweringPhase(hunterWon = false)
                    return
                }
                .onSuccess {
                    Log.d(TAG, "sendNextHunterQuestion: fetched ${it.size} more questions")
                    _hunterAnsweringQuestions.value += it
                }
        }

        hunterQuestionIndex = nextIndex
        hunterJustWrong = false
        Log.d(TAG, "sendNextHunterQuestion: question[$nextIndex]")

        _events.emit(
            GameSessionEvent.HunterAnsweringNextQuestionEvent(
                dto = HunterAnsweringNextQuestionDto(
                    question = _hunterAnsweringQuestions.value[nextIndex].question
                )
            )
        )

        scheduleHunterAnswer()
    }

    private suspend fun finishHunterAnsweringPhase(hunterWon: Boolean) {
        Log.i(
            TAG, "finishHunterAnsweringPhase: hunterWon=$hunterWon" +
                    " | hunterCorrect=$hunterCorrectAnswers totalSteps=$hunterTotalStepsToReach"
        )

        _events.emit(
            GameSessionEvent.HunterAnsweringPhaseFinishedEvent(
                dto = HunterAnsweringPhaseFinishedDto(hunterWon = hunterWon)
            )
        )

        hunterTimerJob?.cancel()
        hunterAnswerJob?.cancel()
        suggestionJob?.cancel()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private fun Float.roundToNearest100(): Float = (roundToInt() / 100 * 100).toFloat()

    private fun simulateBotBoardEarnings(botId: Long): Float {
        val base = botCoinBoosterEarnings[botId] ?: 0f
        val multiplier = Random.nextFloat() * 0.60f + 0.70f
        return (base * multiplier).roundToNearest100()
    }

    private fun BoardQuestion.toDto() = BoardQuestionDto(
        question = question,
        choices = choices,
        correctAnswer = correctAnswer
    )

    private fun CoinBoosterQuestion.toPlayersAnsweringDto() = PlayersAnsweringQuestionDto(
        question = question,
        answer = answer,
        aliases = aliases
    )

    private fun CoinBoosterQuestion.toHunterAnsweringDto() = HunterAnsweringQuestionDto(
        question = question,
        answer = answer,
        aliases = aliases.toMutableList()
    )

    private fun Map<Long, GameSessionPlayer>.updatePlayer(
        id: Long,
        transform: (GameSessionPlayer) -> GameSessionPlayer,
    ): Map<Long, GameSessionPlayer> =
        toMutableMap().also { map -> map[id]?.let { map[id] = transform(it) } }
}