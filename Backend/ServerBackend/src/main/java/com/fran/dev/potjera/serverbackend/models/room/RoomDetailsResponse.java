package com.fran.dev.potjera.serverbackend.models.room;

import com.fran.dev.potjera.potjeradb.enums.RoomStatus;

import java.time.LocalDateTime;
import java.util.List;

public record RoomDetailsResponse(
        String id,
        String code,
        RoomStatus status,
        int maxPlayers,
        int currentPlayers,
        LocalDateTime createdAt,
        List<RoomPlayerDTO> players,
        RoomPlayerDTO hunter
) {
}