package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

public record MoneyOfferRequestPayload(
        float higherOffer,
        float lowerOffer
) {}