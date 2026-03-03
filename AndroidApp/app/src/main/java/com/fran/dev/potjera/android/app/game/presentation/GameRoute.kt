package com.fran.dev.potjera.android.app.game.presentation

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
            enter = fadeIn(tween(200)) + scaleIn(tween(300, easing = EaseOutBack), initialScale = 0.85f)
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