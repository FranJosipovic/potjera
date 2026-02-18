package com.fran.dev.potjerawebscraper.models;

public record ParsedEpisode(
        String link,
        Integer episodeNumber,
        QuizParserResponse data
) {}
