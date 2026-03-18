package com.fran.dev.potjera.serverbackend.controllers;

import com.fran.dev.potjera.potjeradb.repositories.MultipleChoiceQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.QuickFireQuestionRepository;
import com.fran.dev.potjera.serverbackend.models.gamesession.coinbooster.CoinBoosterQuestion;
import com.fran.dev.potjera.serverbackend.models.gamesession.playervhunter.BoardQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuickFireQuestionRepository quickFireRepo;
    private final MultipleChoiceQuestionRepository multipleChoiceRepo;

    @GetMapping("/quick-fire")
    public List<CoinBoosterQuestion> getQuickFireQuestions(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return quickFireRepo.findRandomQuestions(limit)
                .stream()
                .map(q -> CoinBoosterQuestion.builder()
                        .question(q.getQuestion())
                        .answer(q.getAnswer())
                        .aliases(q.getAliases())
                        .build())
                .toList();
    }

    @GetMapping("/multiple-choice")
    public List<BoardQuestion> getMultipleChoiceQuestions(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return multipleChoiceRepo.findRandomQuestions(limit)
                .stream()
                .map(BoardQuestion::from)
                .toList();
    }
}
