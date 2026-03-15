package com.fran.dev.potjera.android.app.room.model


data class JoinPrivateRoomRequest(
    val code: String
)

data class AssignHunterRequest(val hunterId: Long)

data class AssignCaptainRequest(val captainId: Long)

data class CreateRoomRequest(
    val isPrivate: Boolean,
)
