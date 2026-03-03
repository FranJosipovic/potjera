package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

public record BoardPhaseStartingPayload(
        PlayerVHunterGlobalState globalState,
        PlayerVHunterBoardState boardState
) {}
