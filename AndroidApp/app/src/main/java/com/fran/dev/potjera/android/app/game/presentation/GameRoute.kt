package com.fran.dev.potjera.android.app.game.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fran.dev.potjera.android.app.game.hunterphase.presentation.HunterPhaseScreen
import com.fran.dev.potjera.android.app.game.models.enums.GamePhase
import com.fran.dev.potjera.android.app.game.playersphase.presentation.PlayersPhaseScreen
import com.fran.dev.potjera.android.app.game.playervhunter.presentation.PlayerVHunterScreen
import com.fran.dev.potjera.android.app.ui.theme.BgCard
import com.fran.dev.potjera.android.app.ui.theme.BgDeep
import com.fran.dev.potjera.android.app.ui.theme.GradButton
import com.fran.dev.potjera.android.app.ui.theme.Purple
import com.fran.dev.potjera.android.app.ui.theme.TextMuted
import com.fran.dev.potjera.android.app.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun GameRoute(
    gameSessionId: String,
    onNavigateHome: () -> Unit
) {
    val viewModel: GameViewModel = hiltViewModel()

    val isCaptain by viewModel.isCaptain.collectAsStateWithLifecycle()
    val isSpectator by viewModel.isSpectator.collectAsStateWithLifecycle()
    val captainId by viewModel.captainId.collectAsStateWithLifecycle()
    val hunterId by viewModel.hunterId.collectAsStateWithLifecycle()

    val gameSessionState by viewModel.gameSessionState.collectAsStateWithLifecycle()

    val isHunter by viewModel.isHunter.collectAsStateWithLifecycle()
    val isHost by viewModel.isHost.collectAsStateWithLifecycle()
    val coinBooster by viewModel.coinBoosterState.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentCoinBoosterQuestionIndex.collectAsStateWithLifecycle()
    val correctAnswers by viewModel.coinBoosterCorrectAnswers.collectAsStateWithLifecycle()
    val coinsBuilt by viewModel.coinsBuilt.collectAsStateWithLifecycle()
    val timeLeft by viewModel.coinBoosterTimeLeft.collectAsStateWithLifecycle()
    val finishedPlayers by viewModel.coinBoosterFinishedPlayersList.collectAsStateWithLifecycle()
    val gameResults by viewModel.gameResults.collectAsStateWithLifecycle()
    val allPlayers by viewModel.players.collectAsStateWithLifecycle()

    var gameEvent by remember { mutableStateOf<GameEvent?>(null) }


    // board phase
    val playerVHunterBoardState by viewModel.playerVHunterBoardState.collectAsStateWithLifecycle()
    val moneyOffer by viewModel.moneyOfferDto.collectAsStateWithLifecycle()
    val boardPhaseAnsweringPlayerId by viewModel.boardPhaseCurrentPlayerId.collectAsStateWithLifecycle()

    //players answering phase
    val currentAnsweringPlayerId by viewModel.currentAnsweringPlayerId.collectAsStateWithLifecycle()
    val playersAnsweringPlayerList by viewModel.playersAnsweringPlayerList.collectAsStateWithLifecycle()
    val totalSteps by viewModel.totalSteps.collectAsStateWithLifecycle()
    val questionText by viewModel.questionText.collectAsStateWithLifecycle()
    val correctAnswer by viewModel.correctAnswer.collectAsStateWithLifecycle()
    val playerAnsweredCorrectly by viewModel.playerAnsweredCorrectly.collectAsStateWithLifecycle()

    //hunter answering phase
    val hunterAnsweringPhaseState by viewModel.hunterAnsweringPhaseState.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    BackHandler(enabled = gameSessionState.gamePhase != GamePhase.FINISHED) {
        // Block back on all phases except FINISHED
    }

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
                autoDismissMillis = 3000, // does not expire
                onDismiss = { gameEvent = null },
                confirmLabel = "Go Home",
                onConfirm = { gameEvent = null }
            )

            GameEvent.PlayersAnsweringFinished -> GameEventDialog(
                icon = "🏁",
                title = "Players Answering phase finished",
                message = "Players Answering phase finished. Get ready for the next round!, $totalSteps steps in front of hunter",
                autoDismissMillis = 3000, // does not expire
                onDismiss = { gameEvent = null },
            )

            GameEvent.HunterAnsweringFinished -> {

                val iWon =
                    (isHunter && hunterAnsweringPhaseState.hunterAnsweredCorrectly == true) || (!isHunter && hunterAnsweringPhaseState.hunterWon == false)

                val message = if (iWon) {
                    "GAME WON"
                } else {
                    "YOU LOST"
                }

                GameEventDialog(
                    icon = "🏁",
                    title = "Hunter Answering phase finished",
                    message = message,
                    //autoDismissMillis = 3000, // does not expire
                    confirmLabel = "Press this button to go home",
                    onConfirm = {
                        gameEvent = null
                        onNavigateHome()
                    },
                    autoDismissMillis = null,
                    onDismiss = {}
                )
            }
        }
    }


    when (gameSessionState.gamePhase) {
        GamePhase.STARTING -> StartingScreen {
            viewModel.onCountdownFinished()
        }

        GamePhase.COIN_BOOSTER -> if (isHunter) {
            CoinBoosterQueueScreen(
                finishedPlayers = finishedPlayers,
                totalPlayers = gameSessionState.gameSessionPlayers.filter { !it.value.isHunter }.size,
                isHost = isHost,
                onStartBoardQuestions = { viewModel.startBoardQuestions() }
            )
        } else if (coinBooster.isFinished) {
            CoinBoosterQueueScreen(
                finishedPlayers = finishedPlayers,
                totalPlayers = gameSessionState.gameSessionPlayers.filter { !it.value.isHunter }.size,
                isHost = isHost,
                onStartBoardQuestions = { viewModel.startBoardQuestions() }
            )
        } else {
            CoinBoosterScreen(
                question = viewModel.currentQuestion(),
                questionIndex = currentIndex,
                totalQuestions = coinBooster.questions.size,
                correctAnswers = correctAnswers,
                coinsBuilt = coinsBuilt,
                timeLeft = timeLeft,
                onSubmit = { viewModel.submitAnswer(it) },
                goToNextQuestion = { viewModel.nextQuestion() },
            )
        }

        GamePhase.FINISHED -> FinishedScreen(
            results = gameResults,
            myPlayerId = viewModel.myPlayerId,
            onNavigateHome = onNavigateHome
        )

        GamePhase.BOARD -> {
            if (boardPhaseAnsweringPlayerId != null && playerVHunterBoardState != null) {
                PlayerVHunterScreen(
                    myPlayerId = viewModel.myPlayerId,
                    isHunter = isHunter,
                    currentAnsweringPlayerId = boardPhaseAnsweringPlayerId!!,
                    hunterId = hunterId,
                    boardState = playerVHunterBoardState!!,
                    moneyOffer = moneyOffer,
                    allPlayers = allPlayers,
                    onSendMoneyOffer = { hi, lo -> viewModel.sendMoneyOffer(hi, lo) },
                    onAcceptOffer = { viewModel.sendMoneyOfferResponse(it) },
                    onSendAnswer = { viewModel.sendBoardAnswer(it) }
                )
            }
        }

        GamePhase.PLAYERS_ANSWERING -> {
            PlayersPhaseScreen(
                currentAnsweringPlayerId = currentAnsweringPlayerId,
                playersAnsweringPlayerList = playersAnsweringPlayerList,
                playerAnsweredCorrectly = playerAnsweredCorrectly,
                totalSteps = totalSteps,
                questionText = questionText,
                correctAnswer = correctAnswer,
                myPlayerId = viewModel.myPlayerId,
                isSpectator = isSpectator,
                isHunter = isHunter,
                onBuzzIn = { viewModel.buzzIn() },
                onAnswer = { answer -> viewModel.answerQuestion(answer) },
            )
        }

        GamePhase.HUNTER_ANSWERING -> {
            HunterPhaseScreen(
                isHunter = isHunter,
                onAnswer = {
                    if (hunterAnsweringPhaseState.hunterIsAnswering) {
                        viewModel.sendHunterAnswer(it)
                    } else {
                        viewModel.sendPlayersAnswer(it)
                    }
                },
                state = hunterAnsweringPhaseState,
                isCaptain = isCaptain,
                isSpectator = isSpectator,
                suggestions = suggestions,
                captainId = captainId,
                onSuggest = { viewModel.sendSuggestion(it) }
            )
        }
    }
}

@Composable
fun GameEventDialog(
    icon: String,
    title: String,
    message: String,
    autoDismissMillis: Long?,
    onDismiss: () -> Unit,
    accentColor: Color = Purple,
    accentGradient: Brush = GradButton,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null
) {
    // progress for auto-dismiss timer bar
    var progress by remember { mutableFloatStateOf(1f) }

    if (autoDismissMillis != null) {
        LaunchedEffect(Unit) {
            val steps = 60
            val stepMs = autoDismissMillis / steps
            for (i in steps downTo 0) {
                progress = i / steps.toFloat()
                delay(stepMs)
            }
            onDismiss()
        }
    }

    // entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = { if (autoDismissMillis != null) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(
                tween(300, easing = EaseOutBack),
                initialScale = 0.85f
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BgCard)
                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            ) {
                // top glow strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accentGradient)
                )

                Column(
                    modifier = Modifier.padding(top = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(28.dp))

                    // icon circle
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.5.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 34.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = title,
                        color = White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = message,
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    // timer bar OR confirm button
                    if (autoDismissMillis != null) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(BgDeep)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(accentGradient)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    } else if (confirmLabel != null && onConfirm != null) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentGradient)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onConfirm() }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                confirmLabel,
                                color = White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}