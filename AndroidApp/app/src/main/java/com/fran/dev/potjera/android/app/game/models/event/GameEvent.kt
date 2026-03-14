package com.fran.dev.potjera.android.app.game.models.event


sealed class GameEvent {
    data class PlayerWon(val username: String, val money: Float) : GameEvent()
    data class PlayerCaught(val username: String) : GameEvent()
    object BoardPhaseFinished : GameEvent()
    object PlayersAnsweringFinished : GameEvent()
    object HunterAnsweringFinished : GameEvent()
}
