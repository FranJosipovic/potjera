package com.fran.dev.potjera.potjerawebscraper.services;

import com.fran.dev.potjera.potjerawebscraper.builders.ZipBuilder;
import com.fran.dev.potjera.potjerawebscraper.facades.QuizParserFacade;
import com.fran.dev.potjera.potjerawebscraper.models.ParsedEpisode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizExportService {

    private final QuizParserFacade parserFacade;
    private final ZipBuilder zipBuilder;

    public byte[] exportZip(String url) throws Exception {
        List<ParsedEpisode> episodes = parserFacade.parseAllEpisodes(url);
        return zipBuilder.buildZip(episodes);
    }
}

