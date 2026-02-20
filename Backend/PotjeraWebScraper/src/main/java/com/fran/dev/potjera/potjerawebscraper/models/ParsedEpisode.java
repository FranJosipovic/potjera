package com.fran.dev.potjera.potjerawebscraper.models;

public record ParsedEpisode(
        String link,
        Integer episodeNumber,
        QuizParserResponse data
) {}
