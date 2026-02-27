package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BoardQuestion {
    String question;
    List<String> choices;
    String correctAnswer;
}
