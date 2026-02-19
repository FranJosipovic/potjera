package com.fran.dev.potjera.android.app.domain.models.user

data class User(
    val username: String,
    val imageUrl: String?,
    val coins: Int,
    val rank: Int,
    val xp: Int,
    val gamesPlayed: Int,
    val gamesWon: Int,
)