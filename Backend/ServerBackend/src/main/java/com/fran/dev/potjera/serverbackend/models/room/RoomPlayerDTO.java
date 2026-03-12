package com.fran.dev.potjera.serverbackend.models.room;

public record RoomPlayerDTO(
        String id,
        Long playerId,
        String username,
        int rank,
        boolean isHost,
        boolean isReady,
        boolean isHunter,
        boolean isCaptain
) {
}