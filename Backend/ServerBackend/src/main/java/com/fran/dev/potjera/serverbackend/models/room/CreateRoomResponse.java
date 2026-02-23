package com.fran.dev.potjera.serverbackend.models.room;

import com.fran.dev.potjera.potjeradb.enums.RoomStatus;

import java.time.LocalDateTime;

public record CreateRoomResponse(
        String roomId,
        String code,        // null if public
        RoomStatus status,
        int maxPlayers,
        LocalDateTime createdAt
) {}
