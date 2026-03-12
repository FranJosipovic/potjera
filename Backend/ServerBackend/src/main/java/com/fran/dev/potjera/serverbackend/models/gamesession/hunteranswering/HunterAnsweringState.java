package com.fran.dev.potjera.serverbackend.models.gamesession.hunteranswering;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HunterAnsweringState {
    Integer hunterCorrectAnswers;
    Integer totalStepsToReach;             // alivePlayers.size() + playersCorrectAnswers
    Integer currentQuestionIndex;
    boolean hunterJustWrong;        // true = waiting for player counter-answer
}