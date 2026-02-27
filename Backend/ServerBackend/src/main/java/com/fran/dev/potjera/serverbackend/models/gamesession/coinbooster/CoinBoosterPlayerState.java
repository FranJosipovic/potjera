package com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CoinBoosterPlayerState {
    Long playerId;
    Boolean isHunter;
    Integer correctAnswers;
    List<CoinBoosterQuestion> questions;
    Boolean isFinished = false;
}
