package com.fran.dev.potjera.serverbackend.models.room.websocket;

public record PlayerJoinedPayload(
        String roomPlayerId,
        Long playerId,
        String username,
        Boolean isHunter,
        Boolean isReady,
        Boolean isCaptain,
        Integer rank
) {}
