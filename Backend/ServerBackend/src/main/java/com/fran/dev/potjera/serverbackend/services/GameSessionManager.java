package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.serverbackend.models.gamesession.GameSessionState;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameSessionManager {

    private final ConcurrentHashMap<String, GameSessionState> activeSessions
            = new ConcurrentHashMap<>();

    public void saveGameSession(GameSessionState gameSessionState) {
        activeSessions.put(gameSessionState.getGameSessionId(), gameSessionState);
    }

    public GameSessionState getGameSessionState(String gameSessionId) {
        return activeSessions.get(gameSessionId);
    }

    public void removeGameSession(String gameSessionId) {
        activeSessions.remove(gameSessionId);
    }
}
