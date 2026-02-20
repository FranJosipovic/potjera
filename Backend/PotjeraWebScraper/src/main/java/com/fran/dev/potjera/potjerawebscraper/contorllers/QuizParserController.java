package com.fran.dev.potjera.potjerawebscraper.contorllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fran.dev.potjera.potjerawebscraper.models.Items;
import com.fran.dev.potjera.potjerawebscraper.models.QuizParserRequest;
import com.fran.dev.potjera.potjerawebscraper.models.QuizParserResponse;
import com.fran.dev.potjerawebscraper.models.*;
import com.fran.dev.potjera.potjerawebscraper.services.QuizExportService;
import com.fran.dev.potjera.potjerawebscraper.services.QuizParserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz-parser")
public class QuizParserController {

    private final QuizParserService quizParserService;
    private final QuizExportService quizExportService;


    public QuizParserController(QuizParserService quizParserService, QuizExportService quizExportService) {
        this.quizParserService = quizParserService;
        this.quizExportService = quizExportService;
    }

    @PostMapping("/parse-single")
    public ResponseEntity<byte[]> parse(@RequestBody QuizParserRequest request) throws JsonProcessingException {
        try {

            var document = quizParserService.getDocument(request.getUrl());
            if (document == null) return ResponseEntity.badRequest().build();
            var quickFireQuestions = quizParserService.parseQuickFireQuestions(document);
            var multipleChoiceQuestions = quizParserService.parseMultipleChoiceQuestions(document);
            if (multipleChoiceQuestions.isEmpty()) {
                multipleChoiceQuestions = quizParserService.parseMultipleChoiceQuestions2(document);
            }

            var response = new QuizParserResponse(
                    new Items<>(quickFireQuestions, quickFireQuestions.size()),
                    new Items<>(multipleChoiceQuestions, multipleChoiceQuestions.size())
            );

            String filename = request.getFilename() != null ? request.getFilename() : "quiz.json";
            byte[] json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(response);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/parse-main", produces = "application/zip")
    public ResponseEntity<byte[]> export(@RequestParam String url) {
        try {
            byte[] zip = quizExportService.exportZip(url);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=quiz-export.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zip);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
