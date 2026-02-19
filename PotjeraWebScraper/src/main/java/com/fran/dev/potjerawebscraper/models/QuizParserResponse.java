package com.fran.dev.potjerawebscraper.models;


public record QuizParserResponse(
        Items<QuickFireQuestionWeb> quickFireQuestions,
        Items<MultipleChoiceQuestionWeb> multipleChoiceQuestions
) {}

