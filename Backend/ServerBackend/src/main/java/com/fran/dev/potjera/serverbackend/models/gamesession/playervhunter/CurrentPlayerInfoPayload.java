package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

public record CurrentPlayerInfoPayload(
        Long playerId,
        int correctAnswers,
        int coinsEarned
) {}
