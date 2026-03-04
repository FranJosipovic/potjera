package com.fran.dev.potjera.serverbackend.models.gamesession.playersanswering;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlayersAnsweringState {
    Integer correctAnswers = 0;
    Long signedPlayerId = null;
    List<Long> playerIds;
    Integer currentQuestionIndex;    // tracks position in question list
}
