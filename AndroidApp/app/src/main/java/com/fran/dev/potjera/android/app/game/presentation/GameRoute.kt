package com.fran.dev.potjera.android.app.game.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.PlayerVHunterScreen
import kotlinx.coroutines.delay

@Composable
fun GameRoute(
    gameSessionId: String,
    onNavigateHome: () -> Unit
) {
    val viewModel: GameViewModel = hiltViewModel()

    val gamePhase by viewModel.gamePhase.collectAsStateWithLifecycle()
    val isHunter by viewModel.isHunter.collectAsStateWithLifecycle()
    val isHost by viewModel.isHost.collectAsStateWithLifecycle()
    val coinBooster by viewModel.coinBoosterState.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val correctAnswers by viewModel.correctAnswers.collectAsStateWithLifecycle()
    val coinsBuilt by viewModel.coinsBuilt.collectAsStateWithLifecycle()
    val timeLeft by viewModel.timeLeft.collectAsStateWithLifecycle()
    val finishedPlayers by viewModel.finishedPlayers.collectAsStateWithLifecycle()
    val gameResults by viewModel.gameResults.collectAsStateWithLifecycle()
    val totalPlayersCount by viewModel.playersCount.collectAsStateWithLifecycle()
    val allPlayers by viewModel.allPlayers.collectAsStateWithLifecycle()

    var gameEvent by remember { mutableStateOf<GameEvent?>(null) }


    // board phase
    val playerVHunterGlobalState by viewModel.playerVHunterGlobalState.collectAsStateWithLifecycle()
    val playerVHunterBoardState by viewModel.playerVHunterBoardState.collectAsStateWithLifecycle()
    val moneyOffer by viewModel.moneyOffer.collectAsStateWithLifecycle()

    LaunchedEffect(gameSessionId) {
        viewModel.initGameSession(gameSessionId)
    }

    LaunchedEffect(Unit) {
        viewModel.gameEvent.collect { event ->
            gameEvent = event
        }
    }

    gameEvent?.let { event ->
        when (event) {
            is GameEvent.PlayerWon -> GameEventDialog(
                icon = "🏃",
                title = "Player Escaped!",
                message = "${event.username} escaped and brought ${"%.0f".format(event.money)} coins!",
                autoDismissMillis = 2_000,
                onDismiss = { gameEvent = null }
            )
            is GameEvent.PlayerCaught -> GameEventDialog(
                icon = "🎯",
                title = "Player Caught!",
                message = "${event.username} got caught by the Hunter!",
                autoDismissMillis = 2_000,
                onDismiss = { gameEvent = null }
            )
            is GameEvent.BoardPhaseFinished -> GameEventDialog(
                icon = "🏁",
                title = "Round Over",
                message = "The board phase has finished. Get ready for the next round!",
                autoDismissMillis = null, // does not expire
                onDismiss = { gameEvent = null },
                confirmLabel = "Go Home",
                onConfirm = onNavigateHome
            )
        }
    }

    if (gamePhase == GamePhase.BOARD_PHASE) {
        val globalState = playerVHunterGlobalState
        val boardState = playerVHunterBoardState

        if (globalState != null && boardState != null) {
            PlayerVHunterScreen(
                myPlayerId = viewModel.myPlayerId,
                isHunter = isHunter,
                playerVHunterGlobalState = globalState,
                boardState = boardState,
                moneyOffer = moneyOffer,
                allPlayers = allPlayers,
                onSendMoneyOffer = { hi, lo -> viewModel.sendMoneyOffer(hi, lo) },
                onAcceptOffer = { viewModel.sendMoneyOfferResponse(it) },
                onSendAnswer = { viewModel.sendBoardAnswer(it) }
            )
        }
    } else if (isHunter) {
        CoinBoosterQueueScreen(
            finishedPlayers = finishedPlayers,
            totalPlayers = totalPlayersCount,
            isHost = isHost,
            onStartBoardQuestions = { viewModel.startBoardQuestions() }
        )
    } else {
        when (gamePhase) {
            GamePhase.STARTING -> StartingScreen {
                viewModel.onCountdownFinished()
            }

            GamePhase.COIN_BOOSTER -> CoinBoosterScreen(
                question = viewModel.currentQuestion(),
                questionIndex = currentIndex,
                totalQuestions = coinBooster?.questions?.size ?: 0,
                correctAnswers = correctAnswers,
                coinsBuilt = coinsBuilt,
                timeLeft = timeLeft,
                onSubmit = { viewModel.submitAnswer(it) },
                goToNextQuestion = { viewModel.nextQuestion() },
            )

            GamePhase.COIN_BOOSTER_QUEUE -> CoinBoosterQueueScreen(
                finishedPlayers = finishedPlayers,
                totalPlayers = totalPlayersCount,
                isHost = isHost,
                onStartBoardQuestions = { viewModel.startBoardQuestions() }
            )

            GamePhase.FINISHED -> FinishedScreen(
                results = gameResults,
                myPlayerId = viewModel.myPlayerId,
                onNavigateHome = onNavigateHome
            )

            GamePhase.BOARD_PHASE -> {}
        }
    }
}

@Composable
fun GameEventDialog(
    icon: String,
    title: String,
    message: String,
    autoDismissMillis: Long?,         // null = never auto-dismiss
    onDismiss: () -> Unit,
    confirmLabel: String? = null,     // null = no confirm button
    onConfirm: (() -> Unit)? = null
) {
    if (autoDismissMillis != null) {
        LaunchedEffect(Unit) {
            delay(timeMillis = autoDismissMillis)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { if (autoDismissMillis != null) onDismiss() }, // only dismissible if auto-expiring
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = icon, fontSize = 48.sp)
                Text(text = title, style = typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = message, style = typography.bodyMedium, textAlign = TextAlign.Center)

                if (confirmLabel != null && onConfirm != null) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}