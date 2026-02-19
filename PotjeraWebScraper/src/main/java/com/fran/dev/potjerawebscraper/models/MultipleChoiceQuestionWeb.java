package com.fran.dev.potjerawebscraper.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MultipleChoiceQuestionWeb {
    String question;
    String correctAnswer;
    List<String> answers;
}
