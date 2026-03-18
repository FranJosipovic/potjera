package com.fran.dev.potjera.android.app.game.repository

import com.fran.dev.potjera.android.app.game.models.event.GameSessionEvent
import com.fran.dev.potjera.android.app.game.services.GameSessionSocketService
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.SharedFlow

/**
 * Thin wrapper around the socket service.
 * All ViewModels depend on this instead of the socket service directly,
 * so the socket is never touched outside this layer.
 */
@Singleton
class MultiplayerGameSessionRepository @Inject constructor(
    private val socket: GameSessionSocketService
) : GameSessionRepository {

    override val events: SharedFlow<GameSessionEvent> = socket.events

    lateinit var gameSessionId: String

    override fun connect(gameSessionId: String, token: String) {
        this.gameSessionId = gameSessionId
        socket.connect(gameSessionId, token)
    }

    override suspend fun connect(difficulty: Difficulty?) {
        return
    }

    override fun disconnect() = socket.disconnect()

    fun sendConnect() = socket.sendConnect(gameSessionId)

    // ── Coin booster ──────────────────────────────────────────────────────────

    override suspend fun finishCoinBooster(correctAnswers: Int) {
        socket.sendFinish(gameSessionId, correctAnswers)
    }

    // ── Board phase ───────────────────────────────────────────────────────────

    override suspend fun startBoardPhase(moneyWon: Float) {
        socket.sendStartBoardQuestions(gameSessionId)
    }

    override suspend fun sendMoneyOffer(higherOffer: Float, lowerOffer: Float) {
        socket.sendMoneyOffer(gameSessionId, higherOffer, lowerOffer)
    }

    override suspend fun sendMoneyOfferResponse(acceptedOffer: Float) {
        socket.sendMoneyOfferResponse(gameSessionId, acceptedOffer)
    }

    override suspend fun sendBoardAnswer(answer: String, isHunter: Boolean) {
        socket.sendBoardAnswer(gameSessionId, answer, isHunter)
    }

    // ── Board summary ─────────────────────────────────────────────────────────

    override suspend fun startPlayersAnsweringPhase() {
        socket.sendStartPlayersPhase(gameSessionId)
    }

    // ── Players answering phase ───────────────────────────────────────────────

    override suspend fun buzzIn() {
        socket.sendBuzzIn(gameSessionId)
    }

    override suspend fun sendPlayersAnsweringAnswer(answer: String) {
        socket.sendPlayersAnsweringAnswer(gameSessionId, answer)
    }

    // ── Hunter answering phase ────────────────────────────────────────────────

    override suspend fun sendHunterAnsweringAnswer(answer: String) {
        socket.sendHunterAnsweringAnswer(gameSessionId, answer)
    }

    override suspend fun sendPlayerCounterAnswer(answer: String) {
        socket.sendPlayerCounterAnswer(gameSessionId, answer)
    }

    override suspend fun sendSuggestion(suggestion: String) {
        socket.sendSuggestion(gameSessionId, suggestion)
    }
}