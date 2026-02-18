package com.fran.dev.potjerawebscraper.models;


import java.util.List;

public record QuizParserResponse(
        Items<QuickFireQuestion> quickFireQuestions,
        Items<MultipleChoiceQuestion> multipleChoiceQuestions
) {}

