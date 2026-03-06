package com.fran.dev.potjera.serverbackend.models.gamesession.hunteranswering;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HunterAnsweringState {
    Integer hunterCorrectAnswers;
    Integer totalSteps;             // alivePlayers.size() + playersCorrectAnswers
    Long    signedPlayerId;         // non-null only when hunter was wrong & player can answer
    List<Long> playerIds;           // alive players ordered by coins (desc)
    Integer currentQuestionIndex;
    boolean hunterJustWrong;        // true = waiting for player counter-answer
}
