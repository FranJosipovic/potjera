package com.fran.dev.potjera.serverbackend.controllers.websocket;

import com.fran.dev.potjera.serverbackend.models.room.websocket.PlayerJoinedPayload;
import com.fran.dev.potjera.serverbackend.models.room.websocket.RoomEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // client sends to /app/room/{roomId}/join
    @MessageMapping("/room/{roomId}/join")
    public void playerJoined(
            @DestinationVariable String roomId,
            @Payload PlayerJoinedPayload payload
    ) {
        // broadcast to all subscribers of this room
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                new RoomEvent("PLAYER_JOINED", payload)
        );
    }
}
