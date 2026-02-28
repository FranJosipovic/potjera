package com.fran.dev.potjera.serverbackend.controllers.websocket;

import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.MoneyOfferRequestPayload;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.MoneyOfferResponsePayload;
import com.fran.dev.potjera.serverbackend.services.GameSessionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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
    @MessageMapping("/game-session/{gameSessionId}/finish-coin-booster")
    public void onPlayerFinished(
            @DestinationVariable String gameSessionId,
            @Payload CoinBoosterFinishPayload payload,
            Principal principal
    ) {
        if (principal == null) {
            logger.warn("No authenticated user");
            return;
        }

        Long userId = Long.parseLong(principal.getName());

        logger.info("User {} finished coin-booster {}", userId, gameSessionId);

        gameSessionService.finishCoinBoosterSession(
                gameSessionId,
                userId,
                payload.correctAnswers()
        );
    }

    @MessageMapping("/game-session/{gameSessionId}/start-board-phase")
    public void onStartBoardPhase(
            @DestinationVariable String gameSessionId,
            Principal principal
    ) {
        if (principal == null) {
            logger.warn("No authenticated user");
            return;
        }

        Long userId = Long.parseLong(principal.getName());

        logger.info("User {} is starting board phase {}", userId, gameSessionId);

        gameSessionService.startBoardPhase(gameSessionId, userId);
    }

    @MessageMapping("/game-session/{gameSessionId}/player-info-request")
    public void onPlayerInfoRequest(
            @DestinationVariable String gameSessionId,
            Principal principal
    ) {
        if (principal == null) {
            logger.warn("No authenticated user");
            return;
        }

        Long hunterId = Long.parseLong(principal.getName());
        logger.info("Hunter {} requesting player info for session {}", hunterId, gameSessionId);

        gameSessionService.sendCurrentPlayerInfo(gameSessionId, hunterId);
    }

    @MessageMapping("/game-session/{gameSessionId}/money-offer-request")
    public void onMoneyOfferRequest(
            @DestinationVariable String gameSessionId,
            @Payload MoneyOfferRequestPayload payload,
            Principal principal
    ) {
        if (principal == null) {
            logger.warn("No authenticated user");
            return;
        }

        Long hunterId = Long.parseLong(principal.getName());
        logger.info("Hunter {} sent money offer for session {} — higher: {}, lower: {}",
                hunterId, gameSessionId, payload.higherOffer(), payload.lowerOffer());

        gameSessionService.broadcastMoneyOffer(gameSessionId, hunterId, payload);
    }

    @MessageMapping("/game-session/{gameSessionId}/money-offer-response")
    public void onMoneyOfferResponse(
            @DestinationVariable String gameSessionId,
            @Payload MoneyOfferResponsePayload payload,
            Principal principal
    ) {
        if (principal == null) {
            logger.warn("No authenticated user");
            return;
        }

        Long playerId = Long.parseLong(principal.getName());
        logger.info("Player {} responded to money offer for session {} — accepted: {}",
                playerId, gameSessionId, payload.offerAccepted());

        gameSessionService.handleMoneyOfferResponse(gameSessionId, playerId, payload.offerAccepted());
    }

}

