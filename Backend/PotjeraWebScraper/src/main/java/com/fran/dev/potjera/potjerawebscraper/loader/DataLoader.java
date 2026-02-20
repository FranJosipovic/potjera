package com.fran.dev.potjera.potjerawebscraper.loader;

import com.fran.dev.potjera.potjerawebscraper.models.MultipleChoiceQuestionWeb;
import com.fran.dev.potjera.potjerawebscraper.models.QuickFireQuestionWeb;
import com.fran.dev.potjera.potjerawebscraper.models.QuizParserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fran.dev.potjera.potjeradb.models.MultipleChoiceQuestion;
import com.fran.dev.potjera.potjeradb.models.QuickFireQuestion;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import com.fran.dev.potjera.potjeradb.repositories.MultipleChoiceQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.QuickFireQuestionRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements ApplicationRunner {

    private final QuickFireQuestionRepository quickFireRepository;
    private final MultipleChoiceQuestionRepository multipleChoiceRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (quickFireRepository.count() > 0) {
            log.info("Database already populated, skipping data load.");
            return;
        }

        log.info("Populating database from episode JSON files...");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:data/quizzes/*.json");

        for (Resource resource : resources) {
            log.info("Processing: {}", resource.getFilename());
            QuizParserResponse episode = objectMapper.readValue(resource.getInputStream(), QuizParserResponse.class);

            // Quick fire questions
            for (QuickFireQuestionWeb item : episode.quickFireQuestions().items()) {
                quickFireRepository.save(QuickFireQuestion.builder()
                        .question(item.getQuestion())
                        .answer(item.getAnswer().toLowerCase(Locale.ROOT))
                        .aliases(new ArrayList<>())
                        .difficulty(0.5)
                        .build());
            }

            // Multiple choice questions
            for (MultipleChoiceQuestionWeb item : episode.multipleChoiceQuestions().items()) {
                multipleChoiceRepository.save(MultipleChoiceQuestion.builder()
                        .question(item.getQuestion())
                        .correctAnswer(item.getCorrectAnswer())
                        .answers(item.getAnswers())
                        .difficulty(0.5)
                        .build());
            }
        }

        log.info("Data load complete.");
    }
}
