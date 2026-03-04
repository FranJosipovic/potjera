package com.fran.dev.potjera.serverbackend.models.gamesession.playersanswering;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PlayersAnsweringQuestion {
    String question;
    String answer;
    List<String> aliases;
}
