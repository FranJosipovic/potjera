package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.potjeradb.enums.GameStage;
import com.fran.dev.potjera.potjeradb.models.playmode.GameSession;
import com.fran.dev.potjera.potjeradb.models.playmode.Room;
import com.fran.dev.potjera.potjeradb.repositories.GameSessionRepository;
import com.fran.dev.potjera.potjeradb.repositories.QuickFireQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.RoomRepository;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionEvent;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionStage;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionState;
import com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster.CoinBoosterPlayerState;
import com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster.CoinBoosterQuestion;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.CurrentPlayerInfoPayload;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.MoneyOfferRequestPayload;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.PlayerVHunterBoardState;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.PlayerVHunterGlobalState;
import com.fran.dev.potjera.serverbackend.models.room.websocket.GameStartingPayload;
import com.fran.dev.potjera.serverbackend.models.room.websocket.RoomEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collection;
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
                                            .playerName(rp.getPlayer().getDisplayUsername())
                                            .isHost(rp.isHost())
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
        var playersCount = gameSession.getCoinBoosterPlayerStates().size();

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/game-session",
                new GameSessionEvent("COIN_BOOSTER_START", Map.of(
                        "playerState", playerState,
                        "totalPlayersCount", playersCount
                ))
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
                new GameSessionEvent("COIN_BOOSTER_FINISHED", Map.of(
                        "playerId", playerId,
                        "username", playerState.getPlayerName(),
                        "correctAnswers", correctAnswers
                ))
        );

        // check if all non-hunter players finished
        boolean allFinished = gameSession.getCoinBoosterPlayerStates().values().stream()
                .filter(p -> !p.getIsHunter())
                .allMatch(CoinBoosterPlayerState::getIsFinished);

//        if (allFinished) {
//            endGame(gameSessionId, gameSession);
//        }
    }

    public void startBoardPhase(String gameSessionId, Long userId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

        Collection<CoinBoosterPlayerState> playerStates = session.getCoinBoosterPlayerStates().values();

        // find hunter
        Long hunterId = playerStates.stream()
                .filter(CoinBoosterPlayerState::getIsHunter)
                .map(CoinBoosterPlayerState::getPlayerId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hunter found in session"));

        // non-hunter players sorted by correct answers descending
        List<CoinBoosterPlayerState> nonHunterPlayers = playerStates.stream()
                .filter(p -> !p.getIsHunter())
                .sorted(Comparator.comparingInt(CoinBoosterPlayerState::getCorrectAnswers).reversed())
                .toList();

        if (nonHunterPlayers.isEmpty()) {
            throw new IllegalStateException("No players found in session");
        }

        Long firstPlayerId = nonHunterPlayers.getFirst().getPlayerId();

        Map<Long, Float> playersFinishStatus = nonHunterPlayers.stream()
                .collect(Collectors.toMap(
                        CoinBoosterPlayerState::getPlayerId,
                        p -> 0f
                ));

        PlayerVHunterGlobalState boardState = PlayerVHunterGlobalState.builder()
                .hunterId(hunterId)
                .currentPlayerId(firstPlayerId)
                .playersFinishStatus(playersFinishStatus)
                .build();

        Map<Long, PlayerVHunterBoardState> playerBoardStates = nonHunterPlayers.stream()
                .collect(Collectors.toMap(
                        CoinBoosterPlayerState::getPlayerId,
                        p -> PlayerVHunterBoardState.builder()
                                .hunterCorrectAnswers(0)
                                .playerCorrectAnswers(0)
                                .playerStartingIndex(2)
                                .moneyInGame(p.getCorrectAnswers() * 500f)
                                .build()
                ));

        session.setPlayerVHunterGlobalState(boardState);
        session.setPlayerBoardStates(playerBoardStates);
        session.setGameSessionStage(GameSessionStage.PLAYERS_V_HUNTER);
        gameSessionManager.saveGameSession(session);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("BOARD_PHASE_STARTING", boardState)
        );

        logger.info("Board phase started for session {} — first player: {}, hunter: {}",
                gameSessionId, firstPlayerId, hunterId);
    }

    public void sendCurrentPlayerInfo(String gameSessionId, Long hunterId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

        PlayerVHunterGlobalState boardState = session.getPlayerVHunterGlobalState();

        CoinBoosterPlayerState currentPlayer = session.getCoinBoosterPlayerStates()
                .get(boardState.getCurrentPlayerId());

        if (currentPlayer == null) {
            throw new EntityNotFoundException("Current player not found in session");
        }

        CurrentPlayerInfoPayload payload = new CurrentPlayerInfoPayload(
                currentPlayer.getPlayerId(),
                currentPlayer.getCorrectAnswers(),
                currentPlayer.getCorrectAnswers() * 500  // coins earned
        );

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("CURRENT_PLAYER_INFO", payload)
        );

        logger.info("Sent player info for player {} to session {} — correctAnswers: {}, coins: {}",
                currentPlayer.getPlayerId(), gameSessionId,
                currentPlayer.getCorrectAnswers(), currentPlayer.getCorrectAnswers() * 500);
    }

    public void broadcastMoneyOffer(String gameSessionId, Long hunterId, MoneyOfferRequestPayload offer) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

        PlayerVHunterGlobalState boardState = session.getPlayerVHunterGlobalState();

        if (!boardState.getHunterId().equals(hunterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the hunter can make offers");
        }

        Long currentPlayerId = boardState.getCurrentPlayerId();

        // store offer in current player's board state
        PlayerVHunterBoardState playerBoardState = session.getPlayerBoardStates().get(currentPlayerId);

        if (playerBoardState == null) {
            throw new EntityNotFoundException("Board state not found for player: " + currentPlayerId);
        }

        session.getPlayerBoardStates().put(currentPlayerId, playerBoardState);
        gameSessionManager.saveGameSession(session);

        MoneyOfferRequestPayload broadcastPayload = new MoneyOfferRequestPayload(
                offer.higherOffer(),
                offer.lowerOffer()
        );

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("MONEY_OFFER", broadcastPayload)
        );

        logger.info("Money offer broadcast for session {} — currentPlayer: {}, higher: {}, lower: {}",
                gameSessionId, currentPlayerId, offer.higherOffer(), offer.lowerOffer());
    }

    public void handleMoneyOfferResponse(String gameSessionId, Long playerId, float acceptedOffer) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

        PlayerVHunterGlobalState boardState = session.getPlayerVHunterGlobalState();

        if (!boardState.getCurrentPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not your turn");
        }

        PlayerVHunterBoardState playerBoardState = session.getPlayerBoardStates().get(playerId);

        if (playerBoardState == null) {
            throw new EntityNotFoundException("Board state not found for player: " + playerId);
        }

        CoinBoosterPlayerState coinBoosterState = session.getCoinBoosterPlayerStates().get(playerId);
        int correctAnswers = coinBoosterState.getCorrectAnswers();
        float moneyEarned  = correctAnswers * 500f;

        if (acceptedOffer > moneyEarned) {
            playerBoardState.setPlayerStartingIndex(1);
        } else {
            playerBoardState.setPlayerStartingIndex(3);
        }

        playerBoardState.setMoneyInGame(acceptedOffer);
        session.getPlayerBoardStates().put(playerId, playerBoardState);
        gameSessionManager.saveGameSession(session);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("MONEY_OFFER_ACCEPTED", Map.of(
                        "playerId",            playerId,
                        "acceptedOffer",       acceptedOffer,
                        "playerStartingIndex", playerBoardState.getPlayerStartingIndex()
                ))
        );

        logger.info("Player {} accepted offer {} — startingIndex: {}, session: {}",
                playerId, acceptedOffer, playerBoardState.getPlayerStartingIndex(), gameSessionId);
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