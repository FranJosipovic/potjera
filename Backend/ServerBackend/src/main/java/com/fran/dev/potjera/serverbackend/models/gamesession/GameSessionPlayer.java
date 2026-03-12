package com.fran.dev.potjera.serverbackend.models.gamesession;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GameSessionPlayer {
    Long playerId;
    String playerName;
    Float moneyWon;
    Boolean isEliminated;
    Boolean isCaptain;
    Boolean isHunter;
    Boolean isHost;
    Boolean hasPlayedBoard;
}
