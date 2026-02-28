package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
@Builder
public class PlayerVHunterBoardState {
    @Nullable BoardQuestion boardQuestion;
    @Nullable String hunterAnswer;
    @Nullable String playerAnswer;
    Integer hunterCorrectAnswers;
    Integer playerCorrectAnswers;
    Integer playerStartingIndex;
    Float moneyInGame;
}
