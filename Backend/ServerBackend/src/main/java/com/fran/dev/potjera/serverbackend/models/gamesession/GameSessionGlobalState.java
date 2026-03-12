package com.fran.dev.potjera.serverbackend.models.gamesession;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Builder
@Data
public class GameSessionGlobalState {
    String gameSessionId;
    GamePhase gamePhase;
    Map<Long, GameSessionPlayer> gameSessionPlayers;

    public void updateGameSessionPlayer(GameSessionPlayer newPlayer) {
        gameSessionPlayers.put(newPlayer.getPlayerId(), newPlayer);
    }

    public List<GameSessionPlayer> getAlivePlayers() {
        return gameSessionPlayers
                .values()
                .stream()
                .filter(p -> !p.getIsEliminated() && !p.getIsHunter())
                .toList();
    }
}
