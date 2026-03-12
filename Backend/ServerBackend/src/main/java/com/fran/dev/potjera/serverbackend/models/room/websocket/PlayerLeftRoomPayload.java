package com.fran.dev.potjera.serverbackend.models.room.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerLeftRoomPayload {
    Long playerId;
    Long newHunterId;  // nullable — only set if hunter left
    Long newCaptainId;
}
