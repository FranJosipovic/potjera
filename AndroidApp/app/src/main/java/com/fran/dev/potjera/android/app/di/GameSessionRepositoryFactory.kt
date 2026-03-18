package com.fran.dev.potjera.android.app.di

import com.fran.dev.potjera.android.app.game.repository.Difficulty
import com.fran.dev.potjera.android.app.game.repository.GameSessionRepository
import com.fran.dev.potjera.android.app.game.repository.MultiplayerGameSessionRepository
import com.fran.dev.potjera.android.app.game.repository.SingleplayerGameSessionRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * Holds both repository implementations and provides the correct one at
 * runtime based on the game mode selected by the user.
 *
 * Inject this factory into ViewModels or the composable that knows the mode.
 * Call [get] once — the result is stable for the lifetime of the session.
 */
@Singleton
class GameSessionRepositoryFactory @Inject constructor(
    private val multiplayer: MultiplayerGameSessionRepository,
    private val singleplayer: SingleplayerGameSessionRepository,
) {
    fun get(difficulty: Difficulty?): GameSessionRepository = when (difficulty) {
        null -> multiplayer
        else -> singleplayer
    }
}
