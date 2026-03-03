package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
@Builder
public class PlayerVHunterBoardState {
    Boolean questionsStarted;
    Integer currentQuestionIndex;           // ← tracks position in question list
    @Nullable BoardQuestion boardQuestion;  // ← only current question (sent over network)
    @Nullable String hunterAnswer;
    @Nullable String playerAnswer;
    Integer hunterCorrectAnswers;
    Integer playerCorrectAnswers;
    Integer playerStartingIndex;
    Float moneyInGame;
    BoardPhase boardPhase;
}
