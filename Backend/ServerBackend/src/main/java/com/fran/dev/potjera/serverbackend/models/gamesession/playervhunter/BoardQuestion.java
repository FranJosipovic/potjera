package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

import com.fran.dev.potjera.potjeradb.models.MultipleChoiceQuestion;
import lombok.Builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Builder
public record BoardQuestion(
        String question,
        String correctAnswer,
        List<String> choices  // shuffled: correct + wrong answers
) {
    public static BoardQuestion from(MultipleChoiceQuestion q) {

        List<String> allChoices = new ArrayList<>(q.getAnswers());
        Collections.shuffle(allChoices);

        return new BoardQuestion(q.getQuestion(), q.getCorrectAnswer(), allChoices);
    }
}
