package com.fran.dev.potjera.serverbackend.models.room.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptainChangedPayload {
    private String roomPlayerId;
    private Long playerId;
}
