package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionState;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.BoardQuestion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSessionManager {

    private final ConcurrentHashMap<String, GameSessionState> activeSessions
            = new ConcurrentHashMap<>();

    // gameSessionId + playerId -> their board questions
    // key format: "gameSessionId:playerId"
    private final ConcurrentHashMap<String, List<BoardQuestion>> playerBoardQuestions
            = new ConcurrentHashMap<>();

    public void saveGameSession(GameSessionState state) {
        activeSessions.put(state.getGameSessionId(), state);
    }

    public GameSessionState getGameSessionState(String gameSessionId) {
        return activeSessions.get(gameSessionId);
    }

    public void removeGameSession(String gameSessionId) {
        activeSessions.remove(gameSessionId);
        // clean up all question entries for this session
        playerBoardQuestions.keySet().removeIf(k -> k.startsWith(gameSessionId + ":"));
    }

    public void saveBoardQuestions(String gameSessionId, Long playerId, List<BoardQuestion> questions) {
        playerBoardQuestions.put(gameSessionId + ":" + playerId, questions);
    }

    public List<BoardQuestion> getBoardQuestions(String gameSessionId, Long playerId) {
        return playerBoardQuestions.getOrDefault(gameSessionId + ":" + playerId, List.of());
    }
}
