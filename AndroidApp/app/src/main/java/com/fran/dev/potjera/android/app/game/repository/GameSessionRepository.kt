package com.fran.dev.potjera.android.app.game.repository

import com.fran.dev.potjera.android.app.game.models.event.GameSessionEvent
import kotlinx.coroutines.flow.SharedFlow

interface GameSessionRepository {

    val events: SharedFlow<GameSessionEvent>

    fun connect(gameSessionId: String, token: String)
    suspend fun connect(difficulty: Difficulty?)
    fun disconnect()

    // ── Coin booster ──────────────────────────────────────────────────────────
    suspend fun finishCoinBooster(correctAnswers: Int)

    // ── Board phase ───────────────────────────────────────────────────────────
    suspend fun startBoardPhase(moneyWon: Float)
    suspend fun sendMoneyOffer(higherOffer: Float, lowerOffer: Float)
    suspend fun sendMoneyOfferResponse(acceptedOffer: Float)
    suspend fun sendBoardAnswer(answer: String, isHunter: Boolean)

    // ── Board summary ─────────────────────────────────────────────────────────
    suspend fun startPlayersAnsweringPhase()

    // ── Players answering phase ───────────────────────────────────────────────
    suspend fun buzzIn()
    suspend fun sendPlayersAnsweringAnswer(answer: String)

    // ── Hunter answering phase ────────────────────────────────────────────────
    suspend fun sendHunterAnsweringAnswer(answer: String)
    suspend fun sendPlayerCounterAnswer(answer: String)
    suspend fun sendSuggestion(suggestion: String)
}