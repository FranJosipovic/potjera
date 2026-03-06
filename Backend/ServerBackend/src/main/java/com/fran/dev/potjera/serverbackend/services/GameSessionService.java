package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.potjeradb.enums.GameStage;
import com.fran.dev.potjera.potjeradb.models.playmode.GameSession;
import com.fran.dev.potjera.potjeradb.models.playmode.Room;
import com.fran.dev.potjera.potjeradb.repositories.GameSessionRepository;
import com.fran.dev.potjera.potjeradb.repositories.MultipleChoiceQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.QuickFireQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.RoomRepository;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionEvent;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionStage;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionState;
import com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster.CoinBoosterPlayerState;
import com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster.CoinBoosterQuestion;
import com.fran.dev.potjera.serverbackend.models.gamesession.hunteranswering.HunterAnsweringState;
import com.fran.dev.potjera.serverbackend.models.gamesession.playersanswering.PlayersAnsweringQuestion;
import com.fran.dev.potjera.serverbackend.models.gamesession.playersanswering.PlayersAnsweringState;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.*;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameSessionService {
    Logger logger = LoggerFactory.getLogger(GameSessionService.class);

    private final GameSessionRepository gameSessionRepository;
    private final RoomRepository roomRepository;
    private final QuickFireQuestionRepository quickFireQuestionRepository;
    private final MultipleChoiceQuestionRepository multipleChoiceQuestionRepository;
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
                                        quickFireQuestionRepository.findRandomQuestions(4)
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

    //region Coin Booster Phase

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

    //endregion Phase

    //region Board Phase
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

        logger.info("Starting board phase for players {}", playersFinishStatus.keySet());

        Map<Long, String> players = nonHunterPlayers.stream()
                .collect(Collectors.toMap(
                        CoinBoosterPlayerState::getPlayerId,
                        CoinBoosterPlayerState::getPlayerName
                ));

        // ── Global state — who is playing, who is hunter, finish status ───────────
        PlayerVHunterGlobalState globalState = PlayerVHunterGlobalState.builder()
                .hunterId(hunterId)
                .currentPlayerId(firstPlayerId)
                .playersFinishStatus(playersFinishStatus)
                .players(players)
                .build();

        // ── Per-player board state — default phase HUNTER_MAKING_OFFER ───────────
        Map<Long, PlayerVHunterBoardState> playerBoardStates = nonHunterPlayers.stream()
                .collect(Collectors.toMap(
                        CoinBoosterPlayerState::getPlayerId,
                        p -> PlayerVHunterBoardState.builder()
                                .questionsStarted(false)
                                .hunterCorrectAnswers(0)
                                .playerCorrectAnswers(0)
                                .playerStartingIndex(2)
                                .moneyInGame(p.getCorrectAnswers() * 500f)
                                .boardPhase(BoardPhase.HUNTER_MAKING_OFFER)  // ← default phase
                                .build()
                ));

        session.setPlayerVHunterGlobalState(globalState);
        session.setPlayerBoardStates(playerBoardStates);
        session.setGameSessionStage(GameSessionStage.PLAYERS_V_HUNTER);
        gameSessionManager.saveGameSession(session);

        // ── Build broadcast payload — global state + first player's board state ──
        PlayerVHunterBoardState firstPlayerBoardState = playerBoardStates.get(firstPlayerId);

        BoardPhaseStartingPayload payload = new BoardPhaseStartingPayload(
                globalState,
                firstPlayerBoardState
        );

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("BOARD_PHASE_STARTING", payload)
        );

        logger.info("Board phase started for session {} — firstPlayer: {}, hunter: {}",
                gameSessionId, firstPlayerId, hunterId);
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

        PlayerVHunterGlobalState globalState = session.getPlayerVHunterGlobalState();

        if (!globalState.getCurrentPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not your turn");
        }

        PlayerVHunterBoardState playerBoardState = session.getPlayerBoardStates().get(playerId);

        if (playerBoardState == null) {
            throw new EntityNotFoundException("Board state not found for player: " + playerId);
        }

        float moneyEarned = playerBoardState.getMoneyInGame();

        if (acceptedOffer < moneyEarned) {
            playerBoardState.setPlayerStartingIndex(3);
        } else if (acceptedOffer > moneyEarned) {
            playerBoardState.setPlayerStartingIndex(1);
        } else {
            playerBoardState.setPlayerStartingIndex(2);
        }

        playerBoardState.setMoneyInGame(acceptedOffer);
        playerBoardState.setBoardPhase(BoardPhase.OFFER_ACCEPTED);  // ← phase drives Android UI

        session.getPlayerBoardStates().put(playerId, playerBoardState);
        gameSessionManager.saveGameSession(session);

        // broadcast full board state — Android derives everything from boardPhase
        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("MONEY_OFFER_ACCEPTED", playerBoardState)
        );

        logger.info("Player {} accepted offer {} — startingIndex: {}, session: {}",
                playerId, acceptedOffer, playerBoardState.getPlayerStartingIndex(), gameSessionId);

        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() ->
                startBoardSession(gameSessionId, playerId)
        );
    }

    public void startBoardSession(String gameSessionId, Long currentPlayerId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            logger.warn("startBoardSession: session not found {}", gameSessionId);
            return;
        }

        PlayerVHunterBoardState playerBoardState = session.getPlayerBoardStates().get(currentPlayerId);

        if (playerBoardState == null) {
            logger.warn("startBoardSession: board state not found for player {}", currentPlayerId);
            return;
        }

        // fetch + convert questions
        List<BoardQuestion> questions = multipleChoiceQuestionRepository.findRandomQuestions(7)
                .stream()
                .map(BoardQuestion::from)
                .toList();

        if (questions.isEmpty()) {
            logger.error("startBoardSession: no board questions found in db");
            return;
        }

        // store questions separately — not in broadcast payload
        gameSessionManager.saveBoardQuestions(gameSessionId, currentPlayerId, questions);

        // build board state with only first question
        playerBoardState.setQuestionsStarted(true);
        playerBoardState.setCurrentQuestionIndex(0);
        playerBoardState.setHunterCorrectAnswers(0);
        playerBoardState.setPlayerCorrectAnswers(0);
        playerBoardState.setHunterAnswer(null);
        playerBoardState.setPlayerAnswer(null);
        playerBoardState.setBoardQuestion(questions.getFirst());
        playerBoardState.setBoardPhase(BoardPhase.QUESTION_READING);

        session.getPlayerBoardStates().put(currentPlayerId, playerBoardState);
        gameSessionManager.saveGameSession(session);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("NEW_BOARD_QUESTION", playerBoardState)
        );

        logger.info("startBoardSession: first question sent for player {} in session {}",
                currentPlayerId, gameSessionId);
    }

    public void handleBoardAnswer(String gameSessionId, Long userId, String answer, boolean isHunter) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) throw new EntityNotFoundException("Session not found");

        PlayerVHunterGlobalState globalState = session.getPlayerVHunterGlobalState();
        Long currentPlayerId = globalState.getCurrentPlayerId();

        if (!globalState.getCurrentPlayerId().equals(userId) && !isHunter) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not your turn");
        }

        PlayerVHunterBoardState playerBoardState = session.getPlayerBoardStates().get(currentPlayerId);

        if (playerBoardState == null) throw new EntityNotFoundException("Board state not found");

        // record answer
        if (isHunter) {
            playerBoardState.setHunterAnswer(answer);
        } else {
            playerBoardState.setPlayerAnswer(answer);
        }

        playerBoardState.setBoardPhase(BoardPhase.ANSWER_GIVEN);

        String eventType = isHunter
                ? "HUNTER_ANSWERED_BOARD_QUESTION_RES"
                : "PLAYER_ANSWERED_BOARD_QUESTION_RES";

        session.getPlayerBoardStates().put(currentPlayerId, playerBoardState);
        gameSessionManager.saveGameSession(session);

        // broadcast partial state — other player sees the indicator
        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent(eventType, playerBoardState)
        );

        // if both answered → trigger reveal after 2s
        boolean bothAnswered = playerBoardState.getHunterAnswer() != null
                && playerBoardState.getPlayerAnswer() != null;

        if (bothAnswered) {
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() ->
                    revealBoardAnswer(gameSessionId, currentPlayerId)
            );
        }

        logger.info("handleBoardAnswer: {} answered '{}' for player {} in session {}",
                isHunter ? "hunter" : "player", answer, currentPlayerId, gameSessionId);
    }

    public void revealBoardAnswer(String gameSessionId, Long currentPlayerId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterBoardState playerBoardState = session.getPlayerBoardStates().get(currentPlayerId);
        if (playerBoardState == null) return;

        BoardQuestion currentQuestion = playerBoardState.getBoardQuestion();
        String correct = currentQuestion.correctAnswer();

        boolean playerCorrect = correct.equals(playerBoardState.getPlayerAnswer());
        boolean hunterCorrect = correct.equals(playerBoardState.getHunterAnswer());

        if (playerCorrect) {
            playerBoardState.setPlayerCorrectAnswers(playerBoardState.getPlayerCorrectAnswers() + 1);
        }
        if (hunterCorrect) {
            playerBoardState.setHunterCorrectAnswers(playerBoardState.getHunterCorrectAnswers() + 1);
        }

        playerBoardState.setBoardPhase(BoardPhase.ANSWER_REVEAL);
        session.getPlayerBoardStates().put(currentPlayerId, playerBoardState);
        gameSessionManager.saveGameSession(session);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("BOARD_QUESTION_REVEAL", playerBoardState)
        );

        // check win/lose conditions after reveal
        int playerPosition = playerBoardState.getPlayerStartingIndex()
                + playerBoardState.getPlayerCorrectAnswers();
        int hunterPosition = playerBoardState.getHunterCorrectAnswers() - 1;

        if (playerPosition > 6) {
            // ── Player won ────────────────────────────────────────────────────────
            logger.info("revealBoardAnswer: player {} WON in session {}", currentPlayerId, gameSessionId);
            CompletableFuture.delayedExecutor(3500, TimeUnit.MILLISECONDS).execute(() ->
                    handlePlayerWon(gameSessionId, currentPlayerId)
            );
        } else if (hunterPosition >= playerPosition) {
            // ── Hunter caught player ──────────────────────────────────────────────
            logger.info("revealBoardAnswer: player {} CAUGHT by hunter in session {}", currentPlayerId, gameSessionId);
            CompletableFuture.delayedExecutor(3500, TimeUnit.MILLISECONDS).execute(() ->
                    handlePlayerCaught(gameSessionId, currentPlayerId)
            );
        } else {
            // ── Continue — next question ──────────────────────────────────────────
            CompletableFuture.delayedExecutor(3500, TimeUnit.MILLISECONDS).execute(() ->
                    advanceBoardQuestion(gameSessionId, currentPlayerId)
            );
        }
    }

    public void advanceBoardQuestion(String gameSessionId, Long currentPlayerId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterBoardState playerBoardState = session.getPlayerBoardStates().get(currentPlayerId);
        if (playerBoardState == null) return;

        List<BoardQuestion> questions = gameSessionManager.getBoardQuestions(gameSessionId, currentPlayerId);
        int nextIndex = playerBoardState.getCurrentQuestionIndex() + 1;

        if (nextIndex >= questions.size()) {
            logger.info("advanceBoardQuestion: out of questions for player {} — fetching 3 more", currentPlayerId);

            // fetch 3 more questions, excluding already used ones to avoid repeats
            List<String> usedQuestions = questions.stream()
                    .map(BoardQuestion::question)
                    .toList();

            List<BoardQuestion> newQuestions = multipleChoiceQuestionRepository
                    .findRandomQuestionsExcluding(3, usedQuestions)
                    .stream()
                    .map(BoardQuestion::from)
                    .toList();

            if (newQuestions.isEmpty()) {
                logger.warn("advanceBoardQuestion: all questions exhausted, recycling from full pool");
                newQuestions = multipleChoiceQuestionRepository.findRandomQuestions(3)
                        .stream()
                        .map(BoardQuestion::from)
                        .toList();
            }

            // append new questions to existing list
            List<BoardQuestion> extended = new ArrayList<>(questions);
            extended.addAll(newQuestions);
            gameSessionManager.saveBoardQuestions(gameSessionId, currentPlayerId, extended);

            logger.info("advanceBoardQuestion: fetched {} more questions, total now {}",
                    newQuestions.size(), extended.size());
        }

        // re-fetch in case we just extended
        List<BoardQuestion> updatedQuestions = gameSessionManager.getBoardQuestions(gameSessionId, currentPlayerId);

        playerBoardState.setCurrentQuestionIndex(nextIndex);
        playerBoardState.setBoardQuestion(updatedQuestions.get(nextIndex));
        playerBoardState.setHunterAnswer(null);
        playerBoardState.setPlayerAnswer(null);
        playerBoardState.setBoardPhase(BoardPhase.QUESTION_READING);

        session.getPlayerBoardStates().put(currentPlayerId, playerBoardState);
        gameSessionManager.saveGameSession(session);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("NEW_BOARD_QUESTION", playerBoardState)
        );

        logger.info("advanceBoardQuestion: question {} sent for player {} in session {}",
                nextIndex, currentPlayerId, gameSessionId);
    }

    private void handlePlayerWon(String gameSessionId, Long currentPlayerId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterGlobalState globalState = session.getPlayerVHunterGlobalState();
        PlayerVHunterBoardState boardState = session.getPlayerBoardStates().get(currentPlayerId);

        // record win — save money earned
        float moneyWon = boardState.getMoneyInGame();
        globalState.getPlayersFinishStatus().put(currentPlayerId, moneyWon);

        session.setPlayerVHunterGlobalState(globalState);
        gameSessionManager.saveGameSession(session);

        logger.info("handlePlayerWon: player {} won {} coins in session {}",
                currentPlayerId, moneyWon, gameSessionId);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYER_WON", globalState)
        );

        // move to next player after short delay
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() ->
                moveToNextPlayer(gameSessionId, globalState)
        );
    }

    private void handlePlayerCaught(String gameSessionId, Long currentPlayerId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterGlobalState globalState = session.getPlayerVHunterGlobalState();

        // record loss — -1 means caught
        globalState.getPlayersFinishStatus().put(currentPlayerId, -1f);
        session.setPlayerVHunterGlobalState(globalState);
        gameSessionManager.saveGameSession(session);

        logger.info("handlePlayerCaught: player {} caught by hunter in session {}",
                currentPlayerId, gameSessionId);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYER_CAUGHT", globalState)
        );

        // move to next player after short delay
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() ->
                moveToNextPlayer(gameSessionId, globalState)
        );
    }

    private void moveToNextPlayer(String gameSessionId, PlayerVHunterGlobalState globalState) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        Map<Long, Float> finishStatus = globalState.getPlayersFinishStatus();

        logger.info("players left: {}", finishStatus.size());

        Optional<Long> nextPlayer = finishStatus.entrySet().stream()
                .filter(e -> e.getValue() == 0f)
                .map(Map.Entry::getKey)
                .findFirst();

        logger.info("nextPlayer: {}", nextPlayer);

        if (nextPlayer.isEmpty()) {
            endBoardPhase(gameSessionId);
            return;
        }

        Long nextPlayerId = nextPlayer.get();
        globalState.setCurrentPlayerId(nextPlayerId);

        // reset board state for next player to HUNTER_MAKING_OFFER
        PlayerVHunterBoardState nextBoardState = session.getPlayerBoardStates().get(nextPlayerId);
        nextBoardState.setBoardPhase(BoardPhase.HUNTER_MAKING_OFFER);
        nextBoardState.setHunterAnswer(null);
        nextBoardState.setPlayerAnswer(null);
        nextBoardState.setBoardQuestion(null);
        nextBoardState.setQuestionsStarted(false);
        nextBoardState.setHunterCorrectAnswers(0);
        nextBoardState.setPlayerCorrectAnswers(0);
        session.getPlayerBoardStates().put(nextPlayerId, nextBoardState);
        gameSessionManager.saveGameSession(session);

        // same payload shape as BOARD_PHASE_STARTING
        BoardPhaseStartingPayload payload = new BoardPhaseStartingPayload(
                globalState,
                nextBoardState
        );

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("NEXT_PLAYER", payload)
        );

        logger.info("moveToNextPlayer: next player {} in session {}", nextPlayerId, gameSessionId);
    }

    private void endBoardPhase(String gameSessionId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterGlobalState globalState = session.getPlayerVHunterGlobalState();

        logger.info("endBoardPhase: board phase complete for session {}", gameSessionId);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("BOARD_PHASE_FINISHED", globalState)
        );

        session.setGameSessionStage(GameSessionStage.FINISHED);
        gameSessionManager.saveGameSession(session);

        // start players answering phase if at least one player survived
        boolean anyAlive = globalState.getPlayersFinishStatus().values().stream()
                .anyMatch(v -> v != -1f);

        if (anyAlive) {
            CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() ->
                    startPlayersAnsweringPhase(gameSessionId)
            );
        } else {
            logger.info("endBoardPhase: all players caught in session {}, no players answering phase", gameSessionId);
            // TODO: handle all-caught ending
        }
    }
    //endregion

    //region Players Answering Phase
    // ── Called 3s after endBoardPhase if at least one player survived ─────────────
    public void startPlayersAnsweringPhase(String gameSessionId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) {
            logger.warn("startPlayersAnsweringPhase: session not found {}", gameSessionId);
            return;
        }

        PlayerVHunterGlobalState globalState = session.getPlayerVHunterGlobalState();

        // only players who escaped the hunter (finish status != -1)
        List<Long> alivePlayers = globalState.getPlayersFinishStatus().entrySet().stream()
                .filter(e -> e.getValue() != -1f)
                .map(Map.Entry::getKey)
                .toList();

        if (alivePlayers.isEmpty()) {
            logger.info("startPlayersAnsweringPhase: no alive players in session {}, skipping", gameSessionId);
            return;
        }

        // fetch 15 QuickFireQuestions and convert to PlayersAnsweringQuestion via CoinBoosterQuestion
        List<PlayersAnsweringQuestion> questions = quickFireQuestionRepository.findRandomQuestions(15)
                .stream()
                .map(qfq -> PlayersAnsweringQuestion.builder()
                        .question(qfq.getQuestion())
                        .answer(qfq.getAnswer())
                        .aliases(qfq.getAliases() != null ? qfq.getAliases() : List.of())
                        .build())
                .toList();

        if (questions.isEmpty()) {
            logger.error("startPlayersAnsweringPhase: no questions found in db for session {}", gameSessionId);
            return;
        }

        gameSessionManager.savePlayersAnsweringQuestions(gameSessionId, questions);

        PlayersAnsweringState answeringState = PlayersAnsweringState.builder()
                .correctAnswers(0)
                .signedPlayerId(null)
                .playerIds(alivePlayers)
                .currentQuestionIndex(0)
                .build();

        gameSessionManager.savePlayersAnsweringState(gameSessionId, answeringState);
        session.setGameSessionStage(GameSessionStage.PLAYERS_ANSWERING);
        gameSessionManager.saveGameSession(session);

        PlayersAnsweringQuestion firstQuestion = questions.getFirst();

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYERS_ANSWERING_START", Map.of(
                        "playersAnsweringState", answeringState,
                        "question", firstQuestion,
                        "questionNum", 1
                ))
        );

        CompletableFuture.delayedExecutor(2 * 60 * 1000, TimeUnit.MILLISECONDS).execute(() ->
                finishPlayersAnsweringPhase(gameSessionId));

        logger.info("startPlayersAnsweringPhase: started for session {} — {} players, first question sent",
                gameSessionId, alivePlayers.size());
    }

    // ── Player buzzes in — locks the question to them ────────────────────────────
    public void buzzInPlayer(String gameSessionId, Long playerId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) throw new EntityNotFoundException("Session not found: " + gameSessionId);

        PlayersAnsweringState state = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        if (state == null) throw new EntityNotFoundException("Answering state not found for session: " + gameSessionId);

        // already signed in — first buzz wins
        if (state.getSignedPlayerId() != null) {
            logger.info("assignAnsweringPlayer: player {} tried to buzz in but {} already signed in session {}",
                    playerId, state.getSignedPlayerId(), gameSessionId);
            return;
        }

        // must be an alive player
        if (!state.getPlayerIds().contains(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player not eligible for this phase");
        }

        state.setSignedPlayerId(playerId);
        gameSessionManager.savePlayersAnsweringState(gameSessionId, state);

        logger.info("assignAnsweringPlayer: player {} buzzed in for session {}", playerId, gameSessionId);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYER_SIGNED_IN", Map.of(
                        "playerId", playerId
                ))
        );
    }

    // ── Signed-in player submits answer ──────────────────────────────────────────
    public void processPlayerAnswer(String gameSessionId, Long playerId, String answer) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) throw new EntityNotFoundException("Session not found: " + gameSessionId);

        PlayersAnsweringState state = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        if (state == null) throw new EntityNotFoundException("Answering state not found for session: " + gameSessionId);

        if (!playerId.equals(state.getSignedPlayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the signed-in player");
        }

        List<PlayersAnsweringQuestion> questions = gameSessionManager.getPlayersAnsweringQuestions(gameSessionId);
        PlayersAnsweringQuestion current = questions.get(state.getCurrentQuestionIndex());

        String trimmed = answer.trim();

        boolean isCorrect = isFuzzyMatch(trimmed, current.getAnswer())
                || current.getAliases().stream()
                .anyMatch(alias -> isFuzzyMatch(trimmed, alias));

        if (isCorrect) {
            state.setCorrectAnswers(state.getCorrectAnswers() + 1);
            state.setSignedPlayerId(null);
            gameSessionManager.savePlayersAnsweringState(gameSessionId, state);

            logger.info("processPlayerAnswer: player {} correct in session {} — total: {}",
                    playerId, gameSessionId, state.getCorrectAnswers());

            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("PLAYERS_ANSWERING_CORRECT", Map.of(
                            "playerId", playerId,
                            "correctAnswer", current.getAnswer()
                    ))
            );

            CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() ->
                    sendNextQuestion(gameSessionId)
            );
        } else {
            // wrong — release the lock so others can buzz in
            state.setSignedPlayerId(null);
            gameSessionManager.savePlayersAnsweringState(gameSessionId, state);

            logger.info("processPlayerAnswer: player {} wrong in session {}", playerId, gameSessionId);

            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("PLAYERS_ANSWERING_WRONG", Map.of(
                            "playerId", playerId,
                            "correctAnswer", current.getAnswer()
                    ))
            );

            CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() ->
                    sendNextQuestion(gameSessionId)
            );
        }
    }

    public void sendNextQuestion(String gameSessionId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayersAnsweringState state = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        if (state == null) return;

        List<PlayersAnsweringQuestion> questions = gameSessionManager.getPlayersAnsweringQuestions(gameSessionId);
        int nextIndex = state.getCurrentQuestionIndex() + 1;

        // ── Refill if we've exhausted the preloaded batch ─────────────────────
        if (nextIndex >= questions.size()) {
            List<PlayersAnsweringQuestion> more = quickFireQuestionRepository.findRandomQuestions(15)
                    .stream()
                    .map(qfq -> PlayersAnsweringQuestion.builder()
                            .question(qfq.getQuestion())
                            .answer(qfq.getAnswer())
                            .aliases(qfq.getAliases() != null ? qfq.getAliases() : List.of())
                            .build())
                    .toList();

            if (more.isEmpty()) {
                logger.error("sendNextQuestion: no more questions in db for session {}", gameSessionId);
                finishPlayersAnsweringPhase(gameSessionId);
                return;
            }

            List<PlayersAnsweringQuestion> combined = new ArrayList<>(questions);
            combined.addAll(more);
            gameSessionManager.savePlayersAnsweringQuestions(gameSessionId, combined);
            questions = combined;

            logger.info("sendNextQuestion: refilled {} more questions for session {}", more.size(), gameSessionId);
        }

        // reset signed player for new question
        state.setCurrentQuestionIndex(nextIndex);
        state.setSignedPlayerId(null);
        gameSessionManager.savePlayersAnsweringState(gameSessionId, state);

        PlayersAnsweringQuestion next = questions.get(nextIndex);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYERS_ANSWERING_NEXT_QUESTION", Map.of(
                        "question", next,
                        "questionNum", nextIndex + 1,
                        "total", questions.size()
                ))
        );

        logger.info("sendNextQuestion: sent question {} of {} in session {}",
                nextIndex + 1, questions.size(), gameSessionId);
    }

    // ── timeout done — wrap up ──────────────────────────────────────────────
    public void finishPlayersAnsweringPhase(String gameSessionId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayersAnsweringState state = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        if (state == null) return;

        logger.info("finishPlayersAnsweringPhase: session {} finished — correct: {}",
                gameSessionId, state.getCorrectAnswers());

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYERS_ANSWERING_FINISHED", Map.of(
                        "correctAnswers", state.getCorrectAnswers(),
                        "playerIds", state.getPlayerIds()
                ))
        );

        session.setGameSessionStage(GameSessionStage.FINISHED);
        gameSessionManager.saveGameSession(session);

        // TODO: persist results, start next phase
    }

    //endregion

    //region Hunter Answering Phase

    public void startHunterAnsweringPhase(String gameSessionId) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) {
            logger.warn("startHunterAnsweringPhase: session not found {}", gameSessionId);
            return;
        }

        PlayerVHunterGlobalState globalState = session.getPlayerVHunterGlobalState();

        List<Long> alivePlayers = globalState.getPlayersFinishStatus().entrySet().stream()
                .filter(e -> e.getValue() != -1f)
                .map(Map.Entry::getKey)
                .toList();

        if (alivePlayers.isEmpty()) {
            logger.info("startHunterAnsweringPhase: no alive players, skipping {}", gameSessionId);
            return;
        }

        PlayersAnsweringState playersPhaseState = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        int playersCorrectAnswers = playersPhaseState != null ? playersPhaseState.getCorrectAnswers() : 0;
        int totalSteps = alivePlayers.size() + playersCorrectAnswers;

        // order players by coins brought in (desc) — richest gets to counter-answer
        List<Long> playersByCoins = alivePlayers.stream()
                .sorted(Comparator.comparingDouble(
                        (Long pid) -> globalState.getPlayersFinishStatus().getOrDefault(pid, 0f)
                ).reversed())
                .toList();

        List<PlayersAnsweringQuestion> questions = quickFireQuestionRepository.findRandomQuestions(15)
                .stream()
                .map(qfq -> PlayersAnsweringQuestion.builder()
                        .question(qfq.getQuestion())
                        .answer(qfq.getAnswer())
                        .aliases(qfq.getAliases() != null ? qfq.getAliases() : List.of())
                        .build())
                .toList();

        gameSessionManager.savePlayersAnsweringQuestions(gameSessionId + ":hunter", questions);

        HunterAnsweringState state = HunterAnsweringState.builder()
                .hunterCorrectAnswers(0)
                .totalSteps(totalSteps)
                .signedPlayerId(null)
                .playerIds(playersByCoins)
                .currentQuestionIndex(0)
                .hunterJustWrong(false)
                .build();

        gameSessionManager.saveHunterAnsweringState(gameSessionId, state);
        session.setGameSessionStage(GameSessionStage.HUNTER_ANSWERING);
        gameSessionManager.saveGameSession(session);

        PlayersAnsweringQuestion firstQuestion = questions.getFirst();

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("HUNTER_ANSWERING_START", Map.of(
                        "hunterAnsweringState", state,
                        "question", firstQuestion,
                        "questionNum", 1
                ))
        );

        CompletableFuture.delayedExecutor(2 * 60 * 1000, TimeUnit.MILLISECONDS).execute(() ->
                finishHunterAnsweringPhase(gameSessionId, false));

        logger.info("startHunterAnsweringPhase: session {} — totalSteps={}, players={}",
                gameSessionId, totalSteps, playersByCoins.size());
    }

    public void processHunterAnswer(String gameSessionId, Long playerId, String answer) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) throw new EntityNotFoundException("Session not found: " + gameSessionId);

        HunterAnsweringState state = gameSessionManager.getHunterAnsweringState(gameSessionId);
        if (state == null) throw new EntityNotFoundException("Hunter answering state not found: " + gameSessionId);

        // if waiting for player counter-answer, reject hunter input
        if (state.isHunterJustWrong()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Waiting for player counter-answer");
        }

        List<PlayersAnsweringQuestion> questions =
                gameSessionManager.getPlayersAnsweringQuestions(gameSessionId + ":hunter");
        PlayersAnsweringQuestion current = questions.get(state.getCurrentQuestionIndex());

        String trimmed = answer.trim();
        boolean isCorrect = isFuzzyMatch(trimmed, current.getAnswer())
                || current.getAliases().stream().anyMatch(alias -> isFuzzyMatch(trimmed, alias));

        if (isCorrect) {
            int newCorrect = state.getHunterCorrectAnswers() + 1;
            state.setHunterCorrectAnswers(newCorrect);
            gameSessionManager.saveHunterAnsweringState(gameSessionId, state);

            logger.info("processHunterAnswer: hunter correct in session {} — {}/{}",
                    gameSessionId, newCorrect, state.getTotalSteps());

            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("HUNTER_ANSWERING_CORRECT", Map.of(
                            "hunterCorrectAnswers", newCorrect,
                            "totalSteps", state.getTotalSteps(),
                            "correctAnswer", current.getAnswer()
                    ))
            );

            if (newCorrect >= state.getTotalSteps()) {
                CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() ->
                        finishHunterAnsweringPhase(gameSessionId, true));
            } else {
                CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() ->
                        sendNextHunterQuestion(gameSessionId));
            }

        } else {
            // wrong — let richest player counter-answer
            Long richestPlayer = state.getPlayerIds().getFirst();
            state.setHunterJustWrong(true);
            state.setSignedPlayerId(richestPlayer);
            gameSessionManager.saveHunterAnsweringState(gameSessionId, state);

            logger.info("processHunterAnswer: hunter wrong in session {}, signing in player {}",
                    gameSessionId, richestPlayer);

            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("HUNTER_ANSWERING_WRONG", Map.of(
                            "correctAnswer", current.getAnswer(),
                            "signedPlayerId", richestPlayer
                    ))
            );
        }
    }

    public void processPlayerCounterAnswer(String gameSessionId, Long playerId, String answer) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) throw new EntityNotFoundException("Session not found: " + gameSessionId);

        HunterAnsweringState state = gameSessionManager.getHunterAnsweringState(gameSessionId);
        if (state == null) throw new EntityNotFoundException("Hunter answering state not found: " + gameSessionId);

        if (!state.isHunterJustWrong()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not in counter-answer window");
        }
        if (!playerId.equals(state.getSignedPlayerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the signed-in player");
        }

        List<PlayersAnsweringQuestion> questions =
                gameSessionManager.getPlayersAnsweringQuestions(gameSessionId + ":hunter");
        PlayersAnsweringQuestion current = questions.get(state.getCurrentQuestionIndex());

        String trimmed = answer.trim();
        boolean isCorrect = isFuzzyMatch(trimmed, current.getAnswer())
                || current.getAliases().stream().anyMatch(alias -> isFuzzyMatch(trimmed, alias));

        state.setHunterJustWrong(false);
        state.setSignedPlayerId(null);

        if (isCorrect) {
            int hunterCorrect = state.getHunterCorrectAnswers();

            if (hunterCorrect == 0) {
                // hunter hasn't scored yet — push the finish line further
                int newSteps = state.getTotalSteps() + 1;
                state.setTotalSteps(newSteps);
                gameSessionManager.saveHunterAnsweringState(gameSessionId, state);

                logger.info("processPlayerCounterAnswer: player {} correct, hunter at 0 — steps pushed to {} in session {}",
                        playerId, newSteps, gameSessionId);

                messagingTemplate.convertAndSend(
                        "/topic/game-session/" + gameSessionId,
                        new GameSessionEvent("PLAYER_COUNTER_CORRECT", Map.of(
                                "playerId", playerId,
                                "correctAnswer", current.getAnswer(),
                                "totalSteps", newSteps,
                                "effect", "STEPS_INCREASED"
                        ))
                );
            } else {
                // hunter already has points — take one away
                int newCorrect = hunterCorrect - 1;
                state.setHunterCorrectAnswers(newCorrect);
                gameSessionManager.saveHunterAnsweringState(gameSessionId, state);

                logger.info("processPlayerCounterAnswer: player {} correct, hunter steps back to {} in session {}",
                        playerId, newCorrect, gameSessionId);

                messagingTemplate.convertAndSend(
                        "/topic/game-session/" + gameSessionId,
                        new GameSessionEvent("PLAYER_COUNTER_CORRECT", Map.of(
                                "playerId", playerId,
                                "correctAnswer", current.getAnswer(),
                                "hunterCorrectAnswers", newCorrect,
                                "effect", "HUNTER_STEP_REMOVED"
                        ))
                );
            }
        }

        CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() ->
                sendNextHunterQuestion(gameSessionId));
    }

    public void sendNextHunterQuestion(String gameSessionId) {
        HunterAnsweringState state = gameSessionManager.getHunterAnsweringState(gameSessionId);
        if (state == null) return;

        List<PlayersAnsweringQuestion> questions =
                gameSessionManager.getPlayersAnsweringQuestions(gameSessionId + ":hunter");
        int nextIndex = state.getCurrentQuestionIndex() + 1;

        if (nextIndex >= questions.size()) {
            List<PlayersAnsweringQuestion> more = quickFireQuestionRepository.findRandomQuestions(15)
                    .stream()
                    .map(qfq -> PlayersAnsweringQuestion.builder()
                            .question(qfq.getQuestion())
                            .answer(qfq.getAnswer())
                            .aliases(qfq.getAliases() != null ? qfq.getAliases() : List.of())
                            .build())
                    .toList();

            if (more.isEmpty()) {
                finishHunterAnsweringPhase(gameSessionId, false);
                return;
            }

            List<PlayersAnsweringQuestion> combined = new ArrayList<>(questions);
            combined.addAll(more);
            gameSessionManager.savePlayersAnsweringQuestions(gameSessionId + ":hunter", combined);
            questions = combined;
        }

        state.setCurrentQuestionIndex(nextIndex);
        state.setHunterJustWrong(false);
        state.setSignedPlayerId(null);
        gameSessionManager.saveHunterAnsweringState(gameSessionId, state);

        PlayersAnsweringQuestion next = questions.get(nextIndex);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("HUNTER_ANSWERING_NEXT_QUESTION", Map.of(
                        "question", next,
                        "questionNum", nextIndex + 1
                ))
        );
    }

    public void finishHunterAnsweringPhase(String gameSessionId, boolean hunterWon) {
        GameSessionState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        HunterAnsweringState state = gameSessionManager.getHunterAnsweringState(gameSessionId);

        logger.info("finishHunterAnsweringPhase: session {} — hunterWon={}", gameSessionId, hunterWon);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("HUNTER_ANSWERING_FINISHED", Map.of(
                        "hunterWon", hunterWon,
                        "hunterCorrectAnswers", state != null ? state.getHunterCorrectAnswers() : 0,
                        "totalSteps", state != null ? state.getTotalSteps() : 0
                ))
        );

        session.setGameSessionStage(GameSessionStage.FINISHED);
        gameSessionManager.saveGameSession(session);
    }

//endregion

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

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    private boolean isFuzzyMatch(String input, String target) {
        int allowedDistance = target.length() <= 4 ? 0 : target.length() <= 7 ? 1 : 2;
        return levenshtein(input.toLowerCase(), target.toLowerCase()) <= allowedDistance;
    }
}