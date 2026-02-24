package com.fran.dev.potjera.serverbackend.models.room.websocket;

public record RoomEvent(
        String type,
        Object payload
) {}
