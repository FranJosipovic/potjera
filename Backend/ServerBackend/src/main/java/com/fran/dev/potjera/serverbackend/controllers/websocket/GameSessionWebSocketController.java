package com.fran.dev.potjera.serverbackend.controllers.websocket;

import com.fran.dev.potjera.potjeradb.models.User;
import com.fran.dev.potjera.serverbackend.services.GameSessionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameSessionWebSocketController {

    private final GameSessionService gameSessionService;
    Logger logger = LoggerFactory.getLogger(GameSessionWebSocketController.class);

    // called by every player when Game screen opens
    // this triggers COIN_BOOSTER_START for all players
    @MessageMapping("/game-session/{gameSessionId}/connect")
    public void onPlayerConnected(
            @DestinationVariable String gameSessionId,
            Principal principal
    ) {
        if (principal == null) {
            logger.warn("No authenticated user");
            return;
        }

        Long userId = Long.parseLong(principal.getName());

        logger.info("User {} joined {}", userId, gameSessionId);
        gameSessionService.startCoinBoosterSession(gameSessionId, userId);
    }

    // called when player finishes all coin booster questions
    @MessageMapping("/game-session/{gameSessionId}/finish")
    public void onPlayerFinished(
            @DestinationVariable String gameSessionId,
            @Payload CoinBoosterFinishPayload payload,
            @AuthenticationPrincipal User user
    ) {
        gameSessionService.finishCoinBoosterSession(
                gameSessionId,
                user.getId(),
                payload.correctAnswers()
        );
    }
}

