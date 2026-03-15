package com.fran.dev.potjera.android.app.game.repository

import com.fran.dev.potjera.android.app.game.models.event.GameSessionSocketEvent
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
class GameSessionRepository @Inject constructor(
    private val socket: GameSessionSocketService
) {
    val events: SharedFlow<GameSessionSocketEvent> = socket.events

    fun connect(gameSessionId: String, token: String) {
        socket.connect(gameSessionId, token)
    }

    fun disconnect() {
        socket.disconnect()
    }

    // ── Board phase ───────────────────────────────────────────────────────────

    fun sendMoneyOffer(gameSessionId: String, higherOffer: Float, lowerOffer: Float) {
        socket.sendMoneyOffer(gameSessionId, higherOffer, lowerOffer)
    }

    fun sendMoneyOfferResponse(gameSessionId: String, acceptedOffer: Float) {
        socket.sendMoneyOfferResponse(gameSessionId, acceptedOffer)
    }

    fun sendBoardAnswer(gameSessionId: String, answer: String, isHunter: Boolean) {
        socket.sendBoardAnswer(gameSessionId, answer, isHunter)
    }

    fun sendStartBoardQuestions(gameSessionId: String) {
        socket.sendStartBoardQuestions(gameSessionId)
    }

    // ── Coin booster phase ────────────────────────────────────────────────────

    fun sendFinishCoinBooster(gameSessionId: String, correctAnswers: Int) {
        socket.sendFinish(gameSessionId, correctAnswers)
    }

    fun sendConnect(gameSessionId: String) {
        socket.sendConnect(gameSessionId)
    }

    // ── Players answering phase ───────────────────────────────────────────────

    fun sendBuzzIn(gameSessionId: String) {
        socket.sendBuzzIn(gameSessionId)
    }

    fun sendPlayersAnsweringAnswer(gameSessionId: String, answer: String) {
        socket.sendPlayersAnsweringAnswer(gameSessionId, answer)
    }

    // ── Hunter answering phase ────────────────────────────────────────────────

    fun sendHunterAnsweringAnswer(gameSessionId: String, answer: String) {
        socket.sendHunterAnsweringAnswer(gameSessionId, answer)
    }

    fun sendPlayerCounterAnswer(gameSessionId: String, answer: String) {
        socket.sendPlayerCounterAnswer(gameSessionId, answer)
    }

    fun sendSuggestion(gameSessionId: String, suggestion: String) {
        socket.sendSuggestion(gameSessionId, suggestion)
    }
}