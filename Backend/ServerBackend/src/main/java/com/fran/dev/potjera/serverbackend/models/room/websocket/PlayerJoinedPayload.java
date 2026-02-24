package com.fran.dev.potjera.serverbackend.models.room.websocket;

public record PlayerJoinedPayload(
        Long playerId,
        String username,
        Boolean isHunter,
        Integer rank
) {}
