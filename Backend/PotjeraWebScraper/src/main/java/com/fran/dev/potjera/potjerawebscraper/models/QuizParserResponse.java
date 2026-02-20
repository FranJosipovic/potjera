package com.fran.dev.potjera.potjerawebscraper.models;


public record QuizParserResponse(
        Items<QuickFireQuestionWeb> quickFireQuestions,
        Items<MultipleChoiceQuestionWeb> multipleChoiceQuestions
) {}

