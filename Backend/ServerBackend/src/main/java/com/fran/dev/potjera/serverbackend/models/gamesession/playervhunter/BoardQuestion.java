package com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter;

import com.fran.dev.potjera.potjeradb.models.MultipleChoiceQuestion;
import lombok.Builder;
import lombok.Data;

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
        // combine correct + wrong answers and shuffle
        List<String> allChoices = new ArrayList<>(q.getAnswers());
        allChoices.add(q.getCorrectAnswer());
        Collections.shuffle(allChoices);
        // limit to 3 choices for board phase
        List<String> choices = allChoices.stream().limit(3).toList();
        // make sure correct answer is included
        if (!choices.contains(q.getCorrectAnswer())) {
            choices = new ArrayList<>(choices);
            choices.set(2, q.getCorrectAnswer());
            Collections.shuffle(choices);
        }
        return new BoardQuestion(q.getQuestion(), q.getCorrectAnswer(), choices);
    }
}
