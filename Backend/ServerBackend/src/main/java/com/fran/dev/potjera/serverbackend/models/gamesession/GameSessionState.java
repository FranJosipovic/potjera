package com.fran.dev.potjera.serverbackend.models.gamesession;

import com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster.CoinBoosterPlayerState;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.PlayerVHunterBoardState;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.PlayerVHunterGlobalState;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class GameSessionState {
    private String gameSessionId;
    private Map<Long, CoinBoosterPlayerState> coinBoosterPlayerStates;
    private PlayerVHunterGlobalState playerVHunterGlobalState;
    private GameSessionStage gameSessionStage;
    private Map<Long, PlayerVHunterBoardState> playerBoardStates;

    public void updateCoinBoosterPlayerState(CoinBoosterPlayerState newPlayerState) {
        coinBoosterPlayerStates.put(newPlayerState.getPlayerId(), newPlayerState);
    }
}
