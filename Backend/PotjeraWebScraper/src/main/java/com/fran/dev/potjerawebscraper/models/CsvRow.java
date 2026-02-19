package com.fran.dev.potjerawebscraper.models;

public record CsvRow(
        String link,
        String jsonPath,
        int quickFireCount,
        int boardCount,
        int totalQuickFire,
        int totalBoard
) {}

