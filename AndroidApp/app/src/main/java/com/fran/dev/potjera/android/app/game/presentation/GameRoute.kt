package com.fran.dev.potjera.android.app.game.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GameRoute(
    gameSessionId: String,
    onNavigateHome: () -> Unit   // ← passed from NavHost
) {
    val viewModel: GameViewModel = hiltViewModel()

    val gamePhase by viewModel.gamePhase.collectAsStateWithLifecycle()
    val isHunter by viewModel.isHunter.collectAsStateWithLifecycle()
    val coinBooster by viewModel.coinBoosterState.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val correctAnswers by viewModel.correctAnswers.collectAsStateWithLifecycle()
    val coinsBuilt by viewModel.coinsBuilt.collectAsStateWithLifecycle()
    val timeLeft by viewModel.timeLeft.collectAsStateWithLifecycle()
    val finishedPlayers by viewModel.finishedPlayers.collectAsStateWithLifecycle()
    val gameResults by viewModel.gameResults.collectAsStateWithLifecycle()

    LaunchedEffect(gameSessionId) {
        viewModel.initGameSession(gameSessionId)
    }

    if (isHunter) {
        CoinBoosterQueueScreen(finishedPlayers = finishedPlayers)
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
                onSubmit = { answer -> viewModel.submitAnswer(answer) },
                goToNextQuestion = { viewModel.nextQuestion() },
            )

            GamePhase.COIN_BOOSTER_QUEUE -> CoinBoosterQueueScreen(
                finishedPlayers = finishedPlayers
            )

            GamePhase.FINISHED -> FinishedScreen(
                results = gameResults,
                myPlayerId = viewModel.myPlayerId,
                onNavigateHome = onNavigateHome
            )
        }
    }
}
