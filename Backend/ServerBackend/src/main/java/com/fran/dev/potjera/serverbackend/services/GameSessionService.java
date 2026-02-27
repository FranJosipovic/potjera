package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.potjeradb.enums.GameStage;
import com.fran.dev.potjera.potjeradb.models.playmode.GameSession;
import com.fran.dev.potjera.potjeradb.models.playmode.Room;
import com.fran.dev.potjera.potjeradb.repositories.GameSessionRepository;
import com.fran.dev.potjera.potjeradb.repositories.QuickFireQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.RoomRepository;
import com.fran.dev.potjera.serverbackend.models.gamesession.*;
import com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster.CoinBoosterPlayerState;
import com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster.CoinBoosterQuestion;
import com.fran.dev.potjera.serverbackend.models.room.websocket.GameStartingPayload;
import com.fran.dev.potjera.serverbackend.models.room.websocket.RoomEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameSessionService {
    Logger logger = LoggerFactory.getLogger(GameSessionService.class);

    private final GameSessionRepository gameSessionRepository;
    private final RoomRepository roomRepository;
    private final QuickFireQuestionRepository quickFireQuestionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionManager gameSessionManager;

    // called by host → POST /rooms/{roomId}/start
    public void startGame(Room room) {
        Map<Long, CoinBoosterPlayerState> playerStates =
                room.getPlayers().stream()
                        .map(rp -> {
                            CoinBoosterPlayerState.CoinBoosterPlayerStateBuilder builder =
                                    CoinBoosterPlayerState.builder()
                                            .playerId(rp.getPlayer().getId())
                                            .isHunter(rp.isHunter())
                                            .isFinished(rp.isHunter()) // hunter auto-finished
                                            .correctAnswers(0);

                            if (!rp.isHunter()) {
                                List<CoinBoosterQuestion> questions =
                                        quickFireQuestionRepository.findRandomQuestions(10)
                                                .stream()
                                                .map(qfq -> CoinBoosterQuestion.builder()
                                                        .aliases(qfq.getAliases())
                                                        .question(qfq.getQuestion())
                                                        .answer(qfq.getAnswer())
                                                        .build())
                                                .toList();

                                builder.questions(questions);
                            } else {
                                builder.questions(List.of());
                            }

                            return builder.build();
                        })
                        .collect(Collectors.toMap(
                                CoinBoosterPlayerState::getPlayerId,
                                Function.identity()
                        ));

        // persist to DB
        GameSession gameSession = GameSession.builder()
                .room(room)
                .gameStage(GameStage.COIN_BOOSTER)
                .build();
        gameSessionRepository.save(gameSession);

        // save to memory
        GameSessionState gameSessionState = GameSessionState.builder()
                .gameSessionId(gameSession.getId())
                .gameSessionStage(GameSessionStage.STARTING)
                .coinBoosterPlayerStates(playerStates)
                .build();
        gameSessionManager.saveGameSession(gameSessionState);

        // notify lobby → clients navigate to Game screen
        messagingTemplate.convertAndSend(
                "/topic/room/" + room.getId(),
                new RoomEvent("GAME_STARTING", new GameStartingPayload(
                        gameSession.getId(), "Game is starting!"
                ))
        );
    }

    // called when all players have connected to game WS
    public void startCoinBoosterSession(String gameSessionId, Long userId) {
        logger.info("Starting coin booster session for {} and user {}", gameSessionId, userId);

        GameSessionState gameSession = gameSessionManager.getGameSessionState(gameSessionId);

        if (gameSession == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

//        if (gameSession.getGameSessionStage() == GameSessionStage.COIN_BOOSTER) {
//            return;
//        }

        gameSession.setGameSessionStage(GameSessionStage.COIN_BOOSTER);
        gameSessionManager.saveGameSession(gameSession);

        var playerState = gameSession.getCoinBoosterPlayerStates().get(userId);

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/game-session",
                new GameSessionEvent("COIN_BOOSTER_START", playerState)
        );
    }

    // called when a player finishes all their questions
    public void finishCoinBoosterSession(String gameSessionId, Long playerId, int correctAnswers) {
        GameSessionState gameSession = gameSessionManager.getGameSessionState(gameSessionId);

        if (gameSession == null) {
            throw new EntityNotFoundException("Game session not found");
        }

        // update this player's state
        var playerState = gameSession.getCoinBoosterPlayerStates().get(playerId);
        playerState.setCorrectAnswers(correctAnswers);
        playerState.setIsFinished(true);
        gameSession.updateCoinBoosterPlayerState(playerState);

        gameSessionManager.saveGameSession(gameSession);

        // notify all players someone finished
        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYER_FINISHED", Map.of(
                        "playerId", playerId,
                        "correctAnswers", correctAnswers
                ))
        );

        // check if all non-hunter players finished
        boolean allFinished = gameSession.getCoinBoosterPlayerStates().values().stream()
                .filter(p -> !p.getIsHunter())
                .allMatch(CoinBoosterPlayerState::getIsFinished);

        if (allFinished) {
            endGame(gameSessionId, gameSession);
        }
    }

    private void endGame(String gameSessionId, GameSessionState gameSession) {
        // build final results sorted by correct answers
        List<Map<String, Object>> results = gameSession.getCoinBoosterPlayerStates().values().stream()
                .filter(p -> !p.getIsHunter())
                .sorted(Comparator.comparingInt(CoinBoosterPlayerState::getCorrectAnswers).reversed())
                .map(p -> Map.of(
                        "playerId", (Object) p.getPlayerId(),
                        "correctAnswers", p.getCorrectAnswers()
                ))
                .toList();

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("GAME_FINISHED", results)
        );

        // persist to DB
        gameSessionRepository.findById(gameSessionId).ifPresent(gs -> {
            gs.setFinishedAt(LocalDateTime.now());
            gameSessionRepository.save(gs);
        });

        // clean up memory
        gameSessionManager.removeGameSession(gameSessionId);
    }
}