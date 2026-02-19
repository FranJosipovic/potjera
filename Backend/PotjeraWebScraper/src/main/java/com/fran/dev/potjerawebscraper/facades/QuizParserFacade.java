package com.fran.dev.potjerawebscraper.facades;

import com.fran.dev.potjerawebscraper.models.Items;
import com.fran.dev.potjerawebscraper.models.QuizParserResponse;
import com.fran.dev.potjerawebscraper.services.QuizParserService;
import com.fran.dev.potjerawebscraper.models.ParsedEpisode;
import com.fran.dev.potjerawebscraper.utils.EpisodeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizParserFacade {

    private final QuizParserService quizParserService;

    public List<ParsedEpisode> parseAllEpisodes(String url) throws IOException {
        List<String> links = quizParserService.extractEpisodeLinks(url);
        List<ParsedEpisode> result = new ArrayList<>();

        for (String link : links) {
            QuizParserResponse parsed = parseEpisode(link);
            if (parsed == null) continue;

            Integer ep = EpisodeUtils.extractEpisodeNumber(link);
            if (ep == null) continue;

            result.add(new ParsedEpisode(link, ep, parsed));
        }

        return result;
    }

    private QuizParserResponse parseEpisode(String url) {
        var document = quizParserService.getDocument(url);
        if (document == null) return null;

        var quick = quizParserService.parseQuickFireQuestions(document);
        var multi = quizParserService.parseMultipleChoiceQuestions(document);

        if (multi.isEmpty()) {
            multi = quizParserService.parseMultipleChoiceQuestions2(document);
        }

        return new QuizParserResponse(
                new Items<>(quick, quick.size()),
                new Items<>(multi, multi.size())
        );
    }
}

