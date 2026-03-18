package com.fran.dev.potjera.android.app.game.presentation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fran.dev.potjera.android.app.game.coinbooster.CoinBoosterViewModel
import com.fran.dev.potjera.android.app.game.coinbooster.presentation.CoinBoosterQueueScreen
import com.fran.dev.potjera.android.app.game.coinbooster.presentation.CoinBoosterScreen
import com.fran.dev.potjera.android.app.game.gamefinished.GameFinishedViewModel
import com.fran.dev.potjera.android.app.game.gamefinished.presentation.GameFinishedScreen
import com.fran.dev.potjera.android.app.game.hunterphase.HunterAnsweringViewModel
import com.fran.dev.potjera.android.app.game.hunterphase.presentation.HunterPhaseScreen
import com.fran.dev.potjera.android.app.game.models.enums.GamePhase
import com.fran.dev.potjera.android.app.game.playersphase.PlayersAnsweringViewModel
import com.fran.dev.potjera.android.app.game.playersphase.presentation.PlayersPhaseScreen
import com.fran.dev.potjera.android.app.game.playervhunter.BoardEvent
import com.fran.dev.potjera.android.app.game.playervhunter.BoardViewModel
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.BoardPhaseSummaryScreen
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.PlayerVHunterScreen
import com.fran.dev.potjera.android.app.game.presentation.components.GameEventDialog
import com.fran.dev.potjera.android.app.game.repository.Difficulty

@Composable
fun GameRoute(
    gameSessionId: String,
    difficulty: Difficulty?,
    onNavigateHome: () -> Unit
) {
    val TAG = "GameRoute"

    // ── ViewModels ────────────────────────────────────────────────────────────

    val sessionVM:          GameSessionViewModel      = hiltViewModel()
    val coinBoosterVM:      CoinBoosterViewModel      = hiltViewModel()
    val boardVM:            BoardViewModel            = hiltViewModel()
    val playersAnsweringVM: PlayersAnsweringViewModel  = hiltViewModel()
    val hunterAnsweringVM:  HunterAnsweringViewModel   = hiltViewModel()
    val finishedVM:         GameFinishedViewModel      = hiltViewModel()

    // ── Session state ─────────────────────────────────────────────────────────

    val sessionState by sessionVM.state.collectAsStateWithLifecycle()
    val gamePhase     = sessionState.gamePhase

    val isHunter   by sessionVM.isHunter.collectAsStateWithLifecycle()
    val isHost     by sessionVM.isHost.collectAsStateWithLifecycle()
    val isCaptain  by sessionVM.isCaptain.collectAsStateWithLifecycle()
    val isSpectator by sessionVM.isSpectator.collectAsStateWithLifecycle()
    val captainId  by sessionVM.captainId.collectAsStateWithLifecycle()
    val hunterId   by sessionVM.hunterId.collectAsStateWithLifecycle()
    val allPlayers by sessionVM.players.collectAsStateWithLifecycle()

    // ── Init ──────────────────────────────────────────────────────────────────

    LaunchedEffect(gameSessionId) {
        Log.i(TAG, "init: gameSessionId=$gameSessionId difficulty=$difficulty")
        sessionVM.init(gameSessionId, difficulty)
        coinBoosterVM.init(gameSessionId, difficulty)
        playersAnsweringVM.init(gameSessionId, difficulty)
        hunterAnsweringVM.init(gameSessionId, difficulty)
        finishedVM.init(gameSessionId, difficulty)
    }

    LaunchedEffect(sessionState.gameSessionPlayers, gameSessionId) {
        if (sessionState.gameSessionPlayers.isNotEmpty()) {
            Log.d(TAG, "setContext: players=${sessionState.gameSessionPlayers.values.map { it.playerName }}")
            playersAnsweringVM.setContext(gameSessionId, sessionState.gameSessionPlayers)
        }
    }

    val playersAnsweringList by playersAnsweringVM.playerList.collectAsStateWithLifecycle()
    LaunchedEffect(playersAnsweringList) {
        if (playersAnsweringList.isNotEmpty()) {
            Log.d(TAG, "hunterAnsweringVM.setContext: players=${playersAnsweringList.map { it.name }}")
        }
        hunterAnsweringVM.setContext(gameSessionId, playersAnsweringList)
    }

    LaunchedEffect(isHunter) {
        Log.d(TAG, "boardVM.init: isHunter=$isHunter")
        boardVM.init(gameSessionId, difficulty, isHunter)
    }

    // ── Phase change logging ──────────────────────────────────────────────────

    LaunchedEffect(gamePhase) {
        Log.i(TAG, "gamePhase changed → $gamePhase")
    }

    // ── Back handler ──────────────────────────────────────────────────────────

    BackHandler(enabled = gamePhase != GamePhase.FINISHED) {}

    // ── Board events (dialogs) ────────────────────────────────────────────────

    val boardEvent by boardVM.boardEvent.collectAsStateWithLifecycle()

    boardEvent?.let { event ->
        Log.d(TAG, "boardEvent: ${event::class.simpleName}")
        when (event) {
            is BoardEvent.PlayerWon -> GameEventDialog(
                icon              = "🏃",
                title             = "Player Escaped!",
                message           = "${event.username} escaped and brought ${"%.0f".format(event.money)} coins!",
                autoDismissMillis = 2_000,
                onDismiss         = { boardVM.consumeBoardEvent() }
            )
            is BoardEvent.PlayerCaught -> GameEventDialog(
                icon              = "🎯",
                title             = "Player Caught!",
                message           = "${event.username} got caught by the Hunter!",
                autoDismissMillis = 2_000,
                onDismiss         = { boardVM.consumeBoardEvent() }
            )
            BoardEvent.BoardPhaseFinished -> GameEventDialog(
                icon              = "🏁",
                title             = "Round Over",
                message           = "The board phase has finished. Get ready for the next round!",
                autoDismissMillis = 1_000,
                onDismiss         = { boardVM.consumeBoardEvent() }
            )
        }
    }

    // ── Hunter answering finished dialog ──────────────────────────────────────

    val hunterPhaseState      by hunterAnsweringVM.phaseState.collectAsStateWithLifecycle()
    var showHunterFinishedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hunterAnsweringVM.phaseFinished.collect {
            Log.i(TAG, "hunterPhaseFinished: hunterWon=${hunterPhaseState.hunterWon}")
            showHunterFinishedDialog = true
        }
    }

    if (showHunterFinishedDialog) {
        val iWon = (isHunter && hunterPhaseState.hunterAnsweredCorrectly == true) ||
                (!isHunter && hunterPhaseState.hunterWon == false)
        Log.d(TAG, "showHunterFinishedDialog: iWon=$iWon isHunter=$isHunter")
        GameEventDialog(
            icon          = "🏁",
            title         = "Hunter Answering phase finished",
            message       = if (iWon) "GAME WON" else "YOU LOST",
            autoDismissMillis = 3_000,
            confirmLabel  = "Press this button to go home",
            onDismiss     = { showHunterFinishedDialog = false }
        )
    }

    // ── Players answering finished dialog ─────────────────────────────────────

    val totalSteps               by playersAnsweringVM.totalSteps.collectAsStateWithLifecycle()
    var showPlayersFinishedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playersAnsweringVM.phaseFinished.collect {
            Log.i(TAG, "playersAnsweringPhaseFinished: totalSteps=$totalSteps")
            showPlayersFinishedDialog = true
        }
    }

    if (showPlayersFinishedDialog) {
        GameEventDialog(
            icon              = "🏁",
            title             = "Players Answering phase finished",
            message           = "Players Answering phase finished. $totalSteps steps in front of hunter",
            autoDismissMillis = 3_000,
            onDismiss         = { showPlayersFinishedDialog = false }
        )
    }

    // ── Phase routing ─────────────────────────────────────────────────────────

    when (gamePhase) {

        GamePhase.STARTING -> { /* waiting for first event */ }

        GamePhase.COIN_BOOSTER -> {
            val coinBoosterState by coinBoosterVM.playerState.collectAsStateWithLifecycle()
            val currentIndex     by coinBoosterVM.currentQuestionIndex.collectAsStateWithLifecycle()
            val correctAnswers   by coinBoosterVM.correctAnswers.collectAsStateWithLifecycle()
            val coinsBuilt       by coinBoosterVM.coinsBuilt.collectAsStateWithLifecycle()
            val timeLeft         by coinBoosterVM.timeLeft.collectAsStateWithLifecycle()
            val finishedPlayers  by coinBoosterVM.finishedPlayers.collectAsStateWithLifecycle()

            val totalNonHunterPlayers = sessionState.gameSessionPlayers.count { !it.value.isHunter }

            if (isHunter || coinBoosterState.isFinished) {
                CoinBoosterQueueScreen(
                    finishedPlayers    = finishedPlayers,
                    totalPlayers       = totalNonHunterPlayers,
                    isHost             = isHost,
                    onStartBoardQuestions = {
                        Log.i(TAG, "onStartBoardQuestions: coinsBuilt=$coinsBuilt")
                        coinBoosterVM.startBoardQuestions(coinBoosterVM.coinsBuilt.value.toFloat())
                    }
                )
            } else {
                CoinBoosterScreen(
                    question        = coinBoosterVM.currentQuestion(),
                    questionIndex   = currentIndex,
                    totalQuestions  = coinBoosterState.questions.size,
                    correctAnswers  = correctAnswers,
                    coinsBuilt      = coinsBuilt,
                    timeLeft        = timeLeft,
                    onSubmit        = { coinBoosterVM.submitAnswer(it) },
                    goToNextQuestion = { coinBoosterVM.nextQuestion() }
                )
            }
        }

        GamePhase.BOARD -> {
            val boardState               by boardVM.boardState.collectAsStateWithLifecycle()
            val moneyOffer               by boardVM.moneyOffer.collectAsStateWithLifecycle()
            val currentAnsweringPlayerId by boardVM.currentPlayerId.collectAsStateWithLifecycle()

            if (currentAnsweringPlayerId != null && boardState != null) {
                PlayerVHunterScreen(
                    myPlayerId               = sessionVM.myPlayerId,
                    isHunter                 = isHunter,
                    currentAnsweringPlayerId = currentAnsweringPlayerId!!,
                    hunterId                 = hunterId,
                    boardState               = boardState!!,
                    moneyOffer               = moneyOffer,
                    allPlayers               = allPlayers,
                    onSendMoneyOffer         = { hi, lo ->
                        Log.d(TAG, "onSendMoneyOffer: higher=\$$hi lower=\$$lo")
                        boardVM.sendMoneyOffer(hi, lo)
                    },
                    onAcceptOffer = { offer ->
                        Log.i(TAG, "onAcceptOffer: \$$offer")
                        boardVM.sendMoneyOfferResponse(offer)
                    },
                    onSendAnswer = { answer ->
                        Log.d(TAG, "onSendBoardAnswer: \"$answer\"")
                        boardVM.sendBoardAnswer(answer)
                    }
                )
            } else {
                Log.w(TAG, "BOARD phase: waiting for boardState (currentPlayerId=$currentAnsweringPlayerId boardState=${boardState != null})")
            }
        }

        GamePhase.BOARD_SUMMARY -> {
            BoardPhaseSummaryScreen(
                players                    = sessionState.gameSessionPlayers.values.toList(),
                isHost                     = isHost,
                onStartPlayersAnsweringPhase = {
                    Log.i(TAG, "onStartPlayersAnsweringPhase")
                    boardVM.startPlayersAnsweringPhase()
                }
            )
        }

        GamePhase.PLAYERS_ANSWERING -> {
            val currentAnsweringPlayerId by playersAnsweringVM.currentAnsweringPlayerId.collectAsStateWithLifecycle()
            val playerAnsweredCorrectly  by playersAnsweringVM.playerAnsweredCorrectly.collectAsStateWithLifecycle()
            val questionText             by playersAnsweringVM.questionText.collectAsStateWithLifecycle()
            val correctAnswer            by playersAnsweringVM.correctAnswer.collectAsStateWithLifecycle()

            PlayersPhaseScreen(
                currentAnsweringPlayerId  = currentAnsweringPlayerId,
                playersAnsweringPlayerList = playersAnsweringList,
                playerAnsweredCorrectly   = playerAnsweredCorrectly,
                totalSteps                = totalSteps,
                questionText              = questionText,
                correctAnswer             = correctAnswer,
                myPlayerId                = sessionVM.myPlayerId,
                isSpectator               = isSpectator,
                isHunter                  = isHunter,
                onBuzzIn                  = {
                    Log.d(TAG, "onBuzzIn")
                    playersAnsweringVM.buzzIn()
                },
                onAnswer = { answer ->
                    Log.d(TAG, "onPlayersAnsweringAnswer: \"$answer\"")
                    playersAnsweringVM.answerQuestion(answer)
                }
            )
        }

        GamePhase.HUNTER_ANSWERING -> {
            val suggestions by hunterAnsweringVM.suggestions.collectAsStateWithLifecycle()

            HunterPhaseScreen(
                isHunter    = isHunter,
                onAnswer    = { answer ->
                    if (hunterPhaseState.hunterIsAnswering) {
                        Log.d(TAG, "onHunterAnswer: \"$answer\"")
                        hunterAnsweringVM.sendHunterAnswer(answer)
                    } else {
                        Log.d(TAG, "onPlayerCounterAnswer: \"$answer\"")
                        hunterAnsweringVM.sendPlayerCounterAnswer(answer)
                    }
                },
                state       = hunterPhaseState,
                isCaptain   = isCaptain,
                isSpectator = isSpectator,
                suggestions = suggestions,
                captainId   = captainId,
                onSuggest   = { suggestion ->
                    Log.d(TAG, "onSuggest: \"$suggestion\"")
                    hunterAnsweringVM.sendSuggestion(suggestion)
                }
            )
        }

        GamePhase.FINISHED -> {
            val results by finishedVM.results.collectAsStateWithLifecycle()
            Log.d(TAG, "FINISHED: ${results.size} results")
            GameFinishedScreen(
                results         = results,
                myPlayerId      = sessionVM.myPlayerId,
                onNavigateHome  = {
                    Log.i(TAG, "onNavigateHome")
                    onNavigateHome()
                }
            )
        }
    }
}
