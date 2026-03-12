package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.potjeradb.enums.GameStage;
import com.fran.dev.potjera.potjeradb.enums.RoomStatus;
import com.fran.dev.potjera.potjeradb.models.User;
import com.fran.dev.potjera.potjeradb.models.playmode.GameSession;
import com.fran.dev.potjera.potjeradb.models.playmode.Room;
import com.fran.dev.potjera.potjeradb.repositories.GameSessionRepository;
import com.fran.dev.potjera.potjeradb.repositories.MultipleChoiceQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.QuickFireQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.RoomRepository;
import com.fran.dev.potjera.serverbackend.models.gamesession.GamePhase;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionEvent;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionGlobalState;
import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionPlayer;
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
import java.util.concurrent.*;
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
        Map<Long, GameSessionPlayer> players =
                room.getPlayers().stream()
                        .map(rp -> {
                            GameSessionPlayer.GameSessionPlayerBuilder builder =
                                    GameSessionPlayer.builder()
                                            .playerId(rp.getPlayer().getId())
                                            .playerName(rp.getPlayer().getDisplayUsername())
                                            .isHost(rp.isHost())
                                            .isHunter(rp.isHunter())
                                            .isEliminated(false)
                                            .hasPlayedBoard(false)
                                            .moneyWon(0f)
                                            .isCaptain(rp.isCaptain());

                            return builder.build();
                        })
                        .collect(Collectors.toMap(
                                GameSessionPlayer::getPlayerId,
                                Function.identity()
                        ));

        // persist to DB
        GameSession gameSession = GameSession.builder()
                .room(room)
                .gameStage(GameStage.STARTED)
                .build();
        gameSessionRepository.save(gameSession);

        // save to memory
        GameSessionGlobalState gameSessionGlobalState = GameSessionGlobalState.builder()
                .gameSessionId(gameSession.getId())
                .gamePhase(GamePhase.STARTING)
                .gameSessionPlayers(players)
                .build();

        gameSessionManager.saveGameSession(gameSessionGlobalState);

        // notify lobby → clients navigate to Game screen
        messagingTemplate.convertAndSend(
                "/topic/room/" + room.getId(),
                new RoomEvent("GAME_STARTING", new GameStartingPayload(
                        gameSession.getId(),
                        "Game is starting!"
                ))
        );
    }

    //region Coin Booster Phase

    // called when all players have connected to game WS
    public void startCoinBoosterSession(String gameSessionId, Long userId) {
        logger.info("Starting coin booster session for {} and user {}", gameSessionId, userId);

        GameSessionGlobalState gameSession = gameSessionManager.getGameSessionState(gameSessionId);

        if (gameSession == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

        gameSession.setGamePhase(GamePhase.COIN_BOOSTER);
        gameSessionManager.saveGameSession(gameSession);

        var players = gameSession.getGameSessionPlayers();

        var playerState = players.get(userId);
        var isHunter = playerState.getIsHunter();

        if (isHunter) {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/game-session",
                    new GameSessionEvent("COIN_BOOSTER_START_HUNTER", Map.of(
                            "playersInfo", players
                    ))
            );
        } else {

            List<CoinBoosterQuestion> questions =
                    quickFireQuestionRepository.findRandomQuestions(4)
                            .stream()
                            .map(qfq -> CoinBoosterQuestion.builder()
                                    .aliases(qfq.getAliases())
                                    .question(qfq.getQuestion())
                                    .answer(qfq.getAnswer())
                                    .build())
                            .toList();

            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/game-session",
                    new GameSessionEvent("COIN_BOOSTER_START_PLAYER", Map.of(
                            "questions", questions,
                            "playersInfo", players
                    ))
            );
        }
    }

    // called when a player finishes all their questions
    public void finishCoinBoosterSession(String gameSessionId, Long playerId, int correctAnswers) {
        GameSessionGlobalState gameSession = gameSessionManager.getGameSessionState(gameSessionId);

        if (gameSession == null) {
            throw new EntityNotFoundException("Game session not found");
        }

        // update this player's state
        var playerState = gameSession.getGameSessionPlayers().get(playerId);
        var moneyWon = correctAnswers * 500f;
        playerState.setMoneyWon(moneyWon);
        gameSession.updateGameSessionPlayer(playerState);

        gameSessionManager.saveGameSession(gameSession);

        // notify all players someone finished
        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("COIN_BOOSTER_FINISHED", Map.of(
                        "playerId", playerId,
                        "username", playerState.getPlayerName(),
                        "moneyWon", moneyWon
                ))
        );
    }

    //endregion Phase

    //region Board Phase
    public void startBoardPhase(String gameSessionId, Long userId) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

        Collection<GameSessionPlayer> playerStates = session.getGameSessionPlayers().values();

        // find hunter
        Long hunterId = playerStates.stream()
                .filter(GameSessionPlayer::getIsHunter)
                .map(GameSessionPlayer::getPlayerId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hunter found in session"));

        // non-hunter players sorted by correct answers descending
        List<GameSessionPlayer> nonHunterPlayers = playerStates.stream()
                .filter(p -> !p.getIsHunter())
                .sorted(Comparator.comparing(GameSessionPlayer::getIsCaptain).reversed()
                        .thenComparingDouble(p -> -p.getMoneyWon()))
                .toList();

        if (nonHunterPlayers.isEmpty()) {
            throw new IllegalStateException("No players found in session");
        }

        GameSessionPlayer firstPlayer = nonHunterPlayers.getFirst();
        Long firstPlayerId = firstPlayer.getPlayerId();

        // ── Global state — who is playing, who is hunter, finish status ───────────
        PlayerVHunterGlobalState globalState = PlayerVHunterGlobalState.builder()
                .hunterId(hunterId)
                .currentPlayerId(firstPlayerId)
                .build();

        // ── Per-player board state — default phase HUNTER_MAKING_OFFER ───────────
        PlayerVHunterBoardState playerVHunterBoardState = PlayerVHunterBoardState.builder()
                .questionsStarted(false)
                .hunterCorrectAnswers(0)
                .playerCorrectAnswers(0)
                .playerStartingIndex(2)
                .moneyInGame(firstPlayer.getMoneyWon())
                .boardPhase(BoardPhase.HUNTER_MAKING_OFFER)  // ← default phase
                .build();

        session.setGamePhase(GamePhase.BOARD);

        gameSessionManager.savePlayerVHunterGlobalState(gameSessionId, globalState);
        gameSessionManager.savePlayerVHunterBoardState(gameSessionId, playerVHunterBoardState);
        gameSessionManager.saveGameSession(session);

        // ── Build broadcast payload — global state + first player's board state ──

        BoardPhaseStartingPayload payload = new BoardPhaseStartingPayload(
                globalState.getCurrentPlayerId(),
                playerVHunterBoardState
        );

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("BOARD_PHASE_STARTING", payload)
        );

        logger.info("Board phase started for session {} — firstPlayer: {}, hunter: {}",
                gameSessionId, firstPlayerId, hunterId);
    }

    public void broadcastMoneyOffer(String gameSessionId, Long hunterId, MoneyOfferRequestPayload offer) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

        PlayerVHunterGlobalState boardState = gameSessionManager.getPlayerVHunterGlobalState(gameSessionId);

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
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            throw new EntityNotFoundException("Game session not found: " + gameSessionId);
        }

        PlayerVHunterGlobalState globalState = gameSessionManager.getPlayerVHunterGlobalState(gameSessionId);

        if (!globalState.getCurrentPlayerId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not your turn");
        }

        PlayerVHunterBoardState playerBoardState = gameSessionManager.getPlayerVHunterBoardState(gameSessionId);

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

        gameSessionManager.savePlayerVHunterBoardState(gameSessionId, playerBoardState);

        // broadcast full board state — Android derives everything from boardPhase
        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("MONEY_OFFER_ACCEPTED", Map.of(
                        "playerStartingIndex", playerBoardState.getPlayerStartingIndex(),
                        "moneyInGame", acceptedOffer
                ))
        );

        logger.info("Player {} accepted offer {} — startingIndex: {}, session: {}",
                playerId, acceptedOffer, playerBoardState.getPlayerStartingIndex(), gameSessionId);

        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() ->
                startBoardSession(gameSessionId, playerId)
        );
    }

    public void startBoardSession(String gameSessionId, Long currentPlayerId) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) {
            logger.warn("startBoardSession: session not found {}", gameSessionId);
            return;
        }

        PlayerVHunterBoardState playerBoardState = gameSessionManager.getPlayerVHunterBoardState(gameSessionId);

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

        gameSessionManager.savePlayerVHunterBoardState(gameSessionId, playerBoardState);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("NEW_BOARD_QUESTION", Map.of(
                        "question", questions.getFirst().question(),
                        "choices", questions.getFirst().choices(),
                        "correctAnswer", questions.getFirst().correctAnswer()
                ))
        );

        logger.info("startBoardSession: first question sent for player {} in session {}",
                currentPlayerId, gameSessionId);
    }

    public void handleBoardAnswer(String gameSessionId, Long userId, String answer, boolean isHunter) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);

        if (session == null) throw new EntityNotFoundException("Session not found");

        PlayerVHunterGlobalState globalState = gameSessionManager.getPlayerVHunterGlobalState(gameSessionId);
        Long currentPlayerId = globalState.getCurrentPlayerId();

        if (!globalState.getCurrentPlayerId().equals(userId) && !isHunter) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "It's not your turn");
        }

        PlayerVHunterBoardState playerBoardState = gameSessionManager.getPlayerVHunterBoardState(gameSessionId);

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

        gameSessionManager.savePlayerVHunterBoardState(gameSessionId, playerBoardState);

        // broadcast partial state — other player sees the indicator
        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent(eventType, Map.of(
                        "answer", answer
                ))
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
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterBoardState playerBoardState = gameSessionManager.getPlayerVHunterBoardState(gameSessionId);
        if (playerBoardState == null) return;

        BoardQuestion currentQuestion = playerBoardState.getBoardQuestion();
        assert currentQuestion != null;
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
        gameSessionManager.savePlayerVHunterBoardState(gameSessionId, playerBoardState);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("BOARD_QUESTION_REVEAL", Map.of(
                        "hunterAnsweredCorrectly", hunterCorrect,
                        "playerAnsweredCorrectly", playerCorrect
                ))
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
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterBoardState playerBoardState = gameSessionManager.getPlayerVHunterBoardState(gameSessionId);
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

        var boardQuestion = updatedQuestions.get(nextIndex);

        playerBoardState.setCurrentQuestionIndex(nextIndex);
        playerBoardState.setBoardQuestion(boardQuestion);
        playerBoardState.setHunterAnswer(null);
        playerBoardState.setPlayerAnswer(null);
        playerBoardState.setBoardPhase(BoardPhase.QUESTION_READING);

        gameSessionManager.savePlayerVHunterBoardState(gameSessionId, playerBoardState);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("NEW_BOARD_QUESTION", Map.of(
                        "question", boardQuestion.question(),
                        "choices", boardQuestion.choices(),
                        "correctAnswer", boardQuestion.correctAnswer()
                ))
        );

        logger.info("advanceBoardQuestion: question {} sent for player {} in session {}",
                nextIndex, currentPlayerId, gameSessionId);
    }

    private void handlePlayerWon(String gameSessionId, Long currentPlayerId) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterBoardState boardState = gameSessionManager.getPlayerVHunterBoardState(gameSessionId);

        // record win — save money earned
        float moneyWon = boardState.getMoneyInGame();

        var playerState = session.getGameSessionPlayers().get(currentPlayerId);
        playerState.setMoneyWon(moneyWon);
        playerState.setHasPlayedBoard(true);
        session.updateGameSessionPlayer(playerState);
        gameSessionManager.saveGameSession(session);

        logger.info("handlePlayerWon: player {} won {} coins in session {}",
                currentPlayerId, moneyWon, gameSessionId);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYER_WON", Map.of(
                        "playerWonId", currentPlayerId,
                        "moneyWon", moneyWon,
                        "playersListUpdated", session.getGameSessionPlayers()
                ))
        );

        // move to next player after short delay
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() ->
                moveToNextPlayer(gameSessionId, currentPlayerId)
        );
    }

    private void handlePlayerCaught(String gameSessionId, Long currentPlayerId) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterGlobalState globalState = gameSessionManager.getPlayerVHunterGlobalState(gameSessionId);

        // record loss — -1 means caught
        var playerState = session.getGameSessionPlayers().get(currentPlayerId);
        playerState.setMoneyWon(0f);
        playerState.setIsEliminated(true);
        playerState.setHasPlayedBoard(true);
        session.updateGameSessionPlayer(playerState);
        gameSessionManager.saveGameSession(session);


        logger.info("handlePlayerCaught: player {} caught by hunter in session {}",
                currentPlayerId, gameSessionId);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYER_CAUGHT", Map.of(
                        "playerCaughtId", currentPlayerId,
                        "playersListUpdated", session.getGameSessionPlayers()
                ))
        );

        // move to next player after short delay
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() ->
                moveToNextPlayer(gameSessionId, currentPlayerId)
        );
    }

    private void moveToNextPlayer(String gameSessionId, Long currentPlayerId) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterGlobalState globalState = gameSessionManager.getPlayerVHunterGlobalState(gameSessionId);

        Map<Long, GameSessionPlayer> gameSessionPlayers = session.getGameSessionPlayers();

        Optional<GameSessionPlayer> nextPlayer = gameSessionPlayers.values().stream().filter(
                p -> !p.getIsHunter() && !p.getHasPlayedBoard() && !(Objects.equals(p.getPlayerId(), currentPlayerId))
        ).findFirst();

        if (nextPlayer.isEmpty()) {
            endBoardPhase(gameSessionId);
            return;
        }

        Long nextPlayerId = nextPlayer.get().getPlayerId();
        globalState.setCurrentPlayerId(nextPlayerId);
        gameSessionManager.savePlayerVHunterGlobalState(gameSessionId, globalState);

        // reset board state for next player to HUNTER_MAKING_OFFER
        PlayerVHunterBoardState newBoardPhase = PlayerVHunterBoardState.builder()
                .boardPhase(BoardPhase.HUNTER_MAKING_OFFER)
                .hunterAnswer(null)
                .playerAnswer(null)
                .boardQuestion(null)
                .questionsStarted(false)
                .hunterCorrectAnswers(0)
                .playerCorrectAnswers(0)
                .playerStartingIndex(2)
                .moneyInGame(nextPlayer.get().getMoneyWon())
                .build();

        gameSessionManager.savePlayerVHunterBoardState(gameSessionId, newBoardPhase);

        // same payload shape as BOARD_PHASE_STARTING
        BoardPhaseStartingPayload payload = new BoardPhaseStartingPayload(
                globalState.getCurrentPlayerId(),
                newBoardPhase
        );

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("NEXT_PLAYER", payload)
        );

        logger.info("moveToNextPlayer: next player {} in session {}", nextPlayerId, gameSessionId);
    }

    private void endBoardPhase(String gameSessionId) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayerVHunterGlobalState globalState = gameSessionManager.getPlayerVHunterGlobalState(gameSessionId);

        logger.info("endBoardPhase: board phase complete for session {}", gameSessionId);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("BOARD_PHASE_FINISHED", Map.of(
                        "message", "BOARD_PHASE_FINISHED"
                ))
        );

        // start players answering phase if at least one player survived
        boolean anyAlive = session.getGameSessionPlayers()
                .values()
                .stream().anyMatch(p -> !p.getIsEliminated() && !p.getIsHunter());

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
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) {
            logger.warn("startPlayersAnsweringPhase: session not found {}", gameSessionId);
            return;
        }
        // only players who escaped the hunter (finish status != -1)
        List<Long> alivePlayers = session.getAlivePlayers().stream().map(GameSessionPlayer::getPlayerId).toList();

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
                .currentQuestionIndex(0)
                .build();

        gameSessionManager.savePlayersAnsweringState(gameSessionId, answeringState);
        session.setGamePhase(GamePhase.PLAYERS_ANSWERING);
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
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) throw new EntityNotFoundException("Session not found: " + gameSessionId);

        PlayersAnsweringState state = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        if (state == null) throw new EntityNotFoundException("Answering state not found for session: " + gameSessionId);

        // already signed in — first buzz wins
        if (state.getSignedPlayerId() != null) {
            logger.info("assignAnsweringPlayer: player {} tried to buzz in but {} already signed in session {}",
                    playerId, state.getSignedPlayerId(), gameSessionId);
            return;
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
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) throw new EntityNotFoundException("Session not found: " + gameSessionId);

        PlayersAnsweringState state = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        if (state == null) throw new EntityNotFoundException("Answering state not found for session: " + gameSessionId);

        boolean onlyOneAlive = session.getAlivePlayers().size() == 1;

        boolean isAlivePlayer = session.getAlivePlayers()
                .stream()
                .anyMatch(p -> p.getPlayerId().equals(playerId));

        if (!isAlivePlayer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not alive in this session");
        }

        if (!onlyOneAlive && !playerId.equals(state.getSignedPlayerId())) {
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
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
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
                        "question", next
                ))
        );

        logger.info("sendNextQuestion: sent question {} of {} in session {}",
                nextIndex + 1, questions.size(), gameSessionId);
    }

    // ── timeout done — wrap up ──────────────────────────────────────────────
    public void finishPlayersAnsweringPhase(String gameSessionId) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        PlayersAnsweringState state = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        if (state == null) return;

        logger.info("finishPlayersAnsweringPhase: session {} finished — correct: {}",
                gameSessionId, state.getCorrectAnswers());

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("PLAYERS_ANSWERING_FINISHED", Map.of(
                        "correctAnswers", state.getCorrectAnswers()
                ))
        );

        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() ->
                startHunterAnsweringPhase(gameSessionId));
    }

    //endregion

    //region Hunter Answering Phase

    public void startHunterAnsweringPhase(String gameSessionId) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) {
            logger.warn("startHunterAnsweringPhase: session not found {}", gameSessionId);
            return;
        }

        List<Long> alivePlayers = session.getAlivePlayers().stream().map(GameSessionPlayer::getPlayerId).toList();

        if (alivePlayers.isEmpty()) {
            logger.info("startHunterAnsweringPhase: no alive players, skipping {}", gameSessionId);
            return;
        }

        PlayersAnsweringState playersPhaseState = gameSessionManager.getPlayersAnsweringState(gameSessionId);
        int playersCorrectAnswers = playersPhaseState != null ? playersPhaseState.getCorrectAnswers() : 0;
        int totalSteps = alivePlayers.size() + playersCorrectAnswers;

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
                .totalStepsToReach(totalSteps)
                .currentQuestionIndex(0)
                .hunterJustWrong(false)
                .build();

        gameSessionManager.saveHunterAnsweringState(gameSessionId, state);
        session.setGamePhase(GamePhase.HUNTER_ANSWERING);
        gameSessionManager.saveGameSession(session);

        PlayersAnsweringQuestion firstQuestion = questions.getFirst();


        long durationMs = 2 * 60 * 1000L;
        long endTimestamp = System.currentTimeMillis() + durationMs;

        scheduleHunterTimer(gameSessionId, durationMs);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("HUNTER_ANSWERING_START", Map.of(
                        "hunterAnsweringState", state,
                        "question", firstQuestion,
                        "endTimestamp", endTimestamp
                ))
        );


        logger.info("startHunterAnsweringPhase: session {} — totalSteps={}, players={}",
                gameSessionId, totalSteps, alivePlayers.size());
    }

    public void processHunterAnswer(String gameSessionId, Long playerId, String answer) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
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
                    gameSessionId, newCorrect, state.getTotalStepsToReach());

            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("HUNTER_ANSWERING_CORRECT", Map.of(
                            "correctAnswer", current.getAnswer()
                    ))
            );

            if (newCorrect >= state.getTotalStepsToReach()) {
                cancelHunterTimer(gameSessionId);
                CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() ->
                        finishHunterAnsweringPhase(gameSessionId, true));
            } else {
                CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() ->
                        sendNextHunterQuestion(gameSessionId));
            }

        } else {
            state.setHunterJustWrong(true);
            //state.setSignedPlayerId(richestPlayer);
            gameSessionManager.saveHunterAnsweringState(gameSessionId, state);

            pauseHunterTimer(gameSessionId);

            logger.info("processHunterAnswer: hunter wrong in session {}",
                    gameSessionId);

            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("HUNTER_ANSWERING_WRONG", Map.of(
                            "correctAnswer", current.getAnswer()
                    ))
            );
        }
    }

    public void processPlayerCounterAnswer(String gameSessionId, Long playerId, String answer) {
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) throw new EntityNotFoundException("Session not found: " + gameSessionId);

        HunterAnsweringState state = gameSessionManager.getHunterAnsweringState(gameSessionId);
        if (state == null) throw new EntityNotFoundException("Hunter answering state not found: " + gameSessionId);

        if (!state.isHunterJustWrong()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not in counter-answer window");
        }
//        if (!playerId.equals(state.getSignedPlayerId())) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the signed-in player");
//        }

        List<PlayersAnsweringQuestion> questions =
                gameSessionManager.getPlayersAnsweringQuestions(gameSessionId + ":hunter");
        PlayersAnsweringQuestion current = questions.get(state.getCurrentQuestionIndex());

        String trimmed = answer.trim();
        boolean isCorrect = isFuzzyMatch(trimmed, current.getAnswer())
                || current.getAliases().stream().anyMatch(alias -> isFuzzyMatch(trimmed, alias));

        state.setHunterJustWrong(false);
        //state.setSignedPlayerId(null);

        if (isCorrect) {
            int hunterCorrect = state.getHunterCorrectAnswers();

            if (hunterCorrect == 0) {
                // hunter hasn't scored yet — push the finish line further
                int newSteps = state.getTotalStepsToReach() + 1;
                state.setTotalStepsToReach(newSteps);
                gameSessionManager.saveHunterAnsweringState(gameSessionId, state);

                logger.info("processPlayerCounterAnswer: player {} correct, hunter at 0 — steps pushed to {} in session {}",
                        playerId, newSteps, gameSessionId);

                messagingTemplate.convertAndSend(
                        "/topic/game-session/" + gameSessionId,
                        new GameSessionEvent("PLAYER_COUNTER_CORRECT", Map.of(
                                "correctAnswer", current.getAnswer()
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
                                "correctAnswer", current.getAnswer()
                        ))
                );
            }
        } else {
            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("PLAYER_COUNTER_WRONG", Map.of(
                            "correctAnswer", current.getAnswer()
                    ))
            );
        }

        CompletableFuture.delayedExecutor(1500, TimeUnit.MILLISECONDS).execute(() ->
                sendNextHunterQuestion(gameSessionId));

        resumeHunterTimer(gameSessionId);
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
        //state.setSignedPlayerId(null);
        gameSessionManager.saveHunterAnsweringState(gameSessionId, state);


        PlayersAnsweringQuestion next = questions.get(nextIndex);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("HUNTER_ANSWERING_NEXT_QUESTION", Map.of(
                        "question", next.getQuestion()
                ))
        );
    }

    public void finishHunterAnsweringPhase(String gameSessionId, boolean hunterWon) {
        cancelHunterTimer(gameSessionId);
        GameSessionGlobalState session = gameSessionManager.getGameSessionState(gameSessionId);
        if (session == null) return;

        HunterAnsweringState state = gameSessionManager.getHunterAnsweringState(gameSessionId);

        logger.info("finishHunterAnsweringPhase: session {} — hunterWon={}", gameSessionId, hunterWon);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("HUNTER_ANSWERING_FINISHED", Map.of(
                        "hunterWon", hunterWon,
                        "hunterCorrectAnswers", state != null ? state.getHunterCorrectAnswers() : 0,
                        "totalSteps", state != null ? state.getTotalStepsToReach() : 0
                ))
        );

        session.setGamePhase(GamePhase.FINISHED);
        gameSessionManager.saveGameSession(session);

        endGame(gameSessionId);
    }

    public void broadcastSuggestion(String suggestion, String gameSessionId, Long playerId) {

        var playerState = gameSessionManager.getGameSessionState(gameSessionId).getGameSessionPlayers().get(playerId);

        messagingTemplate.convertAndSend(
                "/topic/game-session/" + gameSessionId,
                new GameSessionEvent("SUGGESTION", Map.of(
                        "suggestion", suggestion,
                        "sentBy", playerId,
                        "username", playerState.getPlayerName()
                ))
        );
    }

//endregion

    private void endGame(String gameSessionId) {

        var gameSession = gameSessionRepository.findById(gameSessionId);

        if (gameSession.isPresent()) {

            var session = gameSession.get();

            session.setFinishedAt(LocalDateTime.now());
            session.setGameStage(GameStage.FINISHED);
            gameSessionRepository.save(session);

            var room = roomRepository.findById(session.getRoom().getId())
                    .orElseThrow();

            room.setStatus(RoomStatus.FINISHED);
            roomRepository.save(room);
        }

        gameSessionManager.deleteGameSessionData(gameSessionId);
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

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<String, ScheduledFuture<?>> hunterTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> hunterTimerRemainingMs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> hunterTimerStartedAt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> hunterTimerPausedRemainingMs = new ConcurrentHashMap<>();

    private void scheduleHunterTimer(String gameSessionId, long delayMs) {
        long now = System.nanoTime(); // current time in nanoseconds
        long delayNs = TimeUnit.MILLISECONDS.toNanos(delayMs);

        ScheduledFuture<?> future = scheduler.schedule(
                () -> finishHunterAnsweringPhase(gameSessionId, false),
                delayMs,
                TimeUnit.MILLISECONDS
        );

        hunterTimers.put(gameSessionId, future);
        hunterTimerRemainingMs.put(gameSessionId, delayMs); // still store ms for convenience
        hunterTimerStartedAt.put(gameSessionId, now);       // store nanoTime
    }

    private void pauseHunterTimer(String gameSessionId) {
        ScheduledFuture<?> existing = hunterTimers.remove(gameSessionId);

        if (existing != null && !existing.isDone()) {

            long startedAtNs = hunterTimerStartedAt.getOrDefault(gameSessionId, 0L);
            long elapsedNs = System.nanoTime() - startedAtNs;
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNs);

            long remaining = hunterTimerRemainingMs.getOrDefault(gameSessionId, 0L) - elapsedMs;
            remaining = Math.max(remaining, 0);

            hunterTimerPausedRemainingMs.put(gameSessionId, remaining);

            existing.cancel(false);

            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("HUNTER_TIMER_PAUSED", Map.of(
                            "remainingMs", remaining
                    ))
            );

            logger.info("pauseHunterTimer: session {} — {}ms remaining", gameSessionId, remaining);
        }
    }

    private void resumeHunterTimer(String gameSessionId) {
        long remaining = hunterTimerPausedRemainingMs.getOrDefault(gameSessionId, 0L);

        if (remaining > 0) {
            long endTimestamp = System.currentTimeMillis() + remaining;

            scheduleHunterTimer(gameSessionId, remaining);

            messagingTemplate.convertAndSend(
                    "/topic/game-session/" + gameSessionId,
                    new GameSessionEvent("HUNTER_TIMER_RESUMED", Map.of(
                            "endTimestamp", endTimestamp
                    ))
            );

            logger.info("resumeHunterTimer: session {} — {}ms remaining", gameSessionId, remaining);
        } else {
            finishHunterAnsweringPhase(gameSessionId, false);
        }
    }

    private void cancelHunterTimer(String gameSessionId) {
        ScheduledFuture<?> existing = hunterTimers.remove(gameSessionId);
        if (existing != null) existing.cancel(false);
        hunterTimerRemainingMs.remove(gameSessionId);
        hunterTimerStartedAt.remove(gameSessionId);
    }
}