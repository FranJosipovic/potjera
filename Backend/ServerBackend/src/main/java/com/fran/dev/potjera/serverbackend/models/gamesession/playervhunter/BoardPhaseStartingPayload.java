package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

public record BoardPhaseStartingPayload(
        Long currentPlayerId,
        PlayerVHunterBoardState boardState
) {}
