package com.fran.dev.potjera.potjerawebscraper.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuizParserRequest {
    String url;
    private String filename;
}
