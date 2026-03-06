package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionState;
import com.fran.dev.potjera.serverbackend.models.gamesession.hunteranswering.HunterAnsweringState;
import com.fran.dev.potjera.serverbackend.models.gamesession.playersanswering.PlayersAnsweringQuestion;
import com.fran.dev.potjera.serverbackend.models.gamesession.playersanswering.PlayersAnsweringState;
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

    // NEW — players answering phase state per session
    private final ConcurrentHashMap<String, PlayersAnsweringState> playersAnsweringStates
            = new ConcurrentHashMap<>();

    // NEW — questions for players answering phase per session
    private final ConcurrentHashMap<String, List<PlayersAnsweringQuestion>> playersAnsweringQuestions
            = new ConcurrentHashMap<>();

    public void saveGameSession(GameSessionState state) {
        activeSessions.put(state.getGameSessionId(), state);
    }

    public GameSessionState getGameSessionState(String gameSessionId) {
        return activeSessions.get(gameSessionId);
    }

    private final ConcurrentHashMap<String, HunterAnsweringState> hunterAnsweringStates
            = new ConcurrentHashMap<>();

    public void saveHunterAnsweringState(String gameSessionId, HunterAnsweringState state) {
        hunterAnsweringStates.put(gameSessionId, state);
    }

    public HunterAnsweringState getHunterAnsweringState(String gameSessionId) {
        return hunterAnsweringStates.get(gameSessionId);
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

    // players answering phase (per session)
    public void savePlayersAnsweringState(String gameSessionId, PlayersAnsweringState state) {
        playersAnsweringStates.put(gameSessionId, state);
    }

    public PlayersAnsweringState getPlayersAnsweringState(String gameSessionId) {
        return playersAnsweringStates.get(gameSessionId);
    }

    public void savePlayersAnsweringQuestions(String gameSessionId, List<PlayersAnsweringQuestion> questions) {
        playersAnsweringQuestions.put(gameSessionId, questions);
    }

    public List<PlayersAnsweringQuestion> getPlayersAnsweringQuestions(String gameSessionId) {
        return playersAnsweringQuestions.getOrDefault(gameSessionId, List.of());
    }
}
