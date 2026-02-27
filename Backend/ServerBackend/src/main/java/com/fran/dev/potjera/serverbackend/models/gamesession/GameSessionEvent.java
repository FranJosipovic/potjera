package com.fran.dev.potjera.serverbackend.models.gamesession;

public record GameSessionEvent(
        String type,
        Object payload
) {
}
