package com.fran.dev.potjerawebscraper.services;

import com.fran.dev.potjerawebscraper.models.MultipleChoiceQuestion;
import com.fran.dev.potjerawebscraper.models.QuickFireQuestion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class QuizParserService {
    public @Nullable Document getDocument(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10_000)
                    .get();
        } catch (IOException e) {
            System.err.println("Failed to fetch document from URL: " + url);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parses type 1: <div class="quiz-question" data-correct="B">
     * Contains a question <p> and multiple <button class="answer"> elements.
     */
    public List<MultipleChoiceQuestion> parseMultipleChoiceQuestions(Document doc) {
        List<MultipleChoiceQuestion> results = new ArrayList<>();

        Elements questionDivs = doc.select("div.quiz-question");

        for (Element div : questionDivs) {
            String question = div.selectFirst("p.question").text();
            String correctLetter = div.attr("data-correct"); // e.g. "B"

            List<String> answers = new ArrayList<>();
            String correctAnswerText = null;

            for (Element btn : div.select("button.answer")) {
                String answerText = btn.text();
                answers.add(answerText);
                if (btn.attr("data-answer").equals(correctLetter)) {
                    correctAnswerText = answerText;
                }
            }

            results.add(new MultipleChoiceQuestion(question, correctAnswerText, answers));
        }

        return results;
    }
    public List<MultipleChoiceQuestion> parseMultipleChoiceQuestions2(Document doc) {
        List<MultipleChoiceQuestion> results = new ArrayList<>();

        // Handles both <li><p>...</p></li> and <li>direct text</li>
        Elements containers = doc.select("div.elementor-widget-container ul li p, div.elementor-widget-container ul li:not(:has(p))");

        for (Element container : containers) {
            Element br = container.selectFirst("br");
            if (br == null) continue;

            // Build question from all nodes before <br>
            StringBuilder questionBuilder = new StringBuilder();
            for (Node node : container.childNodes()) {
                if (node.equals(br)) break;
                if (node instanceof TextNode tn) {
                    questionBuilder.append(tn.text());
                } else if (node instanceof Element el) {
                    questionBuilder.append(el.text());
                }
            }
            String question = questionBuilder.toString().trim();
            if (question.isEmpty()) continue;

            // Correct answer: <strong> or <b>
            Element correctEl = container.selectFirst("strong, b");
            if (correctEl == null) continue;
            String correctAnswer = correctEl.text().trim();

            // Build answers from all nodes after <br>
            StringBuilder answersBuilder = new StringBuilder();
            boolean afterBr = false;
            for (Node node : container.childNodes()) {
                if (node.equals(br)) { afterBr = true; continue; }
                if (!afterBr) continue;
                if (node instanceof TextNode tn) {
                    answersBuilder.append(tn.text());
                } else if (node instanceof Element el) {
                    answersBuilder.append(el.text());
                }
            }

            String answersSection = answersBuilder.toString().trim();
            List<String> answers = Arrays.stream(answersSection.split("\\s+(?=[a-d]\\))"))
                    .map(s -> s.replaceAll("^[a-d]\\)\\s*", "").trim())
                    .filter(s -> !s.isEmpty())
                    .toList();

            results.add(new MultipleChoiceQuestion(question, correctAnswer, answers));
        }

        return results;
    }

    /**
     * Parses type 2: <p> containing a <b> question and a <span class="quiz-answer"> with the answer.
     */
    public List<QuickFireQuestion> parseQuickFireQuestions(Document doc) {
        List<QuickFireQuestion> results = new ArrayList<>();

        // Select all <p> tags that contain a span.quiz-answer
        Elements paragraphs = doc.select("p:has(span.quiz-answer)");

        for (Element p : paragraphs) {
            // The question is in the first <b> tag (before the span)
            Element questionEl = p.selectFirst("b");
            Element answerEl = p.selectFirst("span.quiz-answer b");

            if (questionEl == null || answerEl == null) continue;

            String question = questionEl.text();
            String answer = answerEl.text();

            results.add(new QuickFireQuestion(question, answer));
        }

        return results;
    }

    private static final Pattern EP_PATTERN =
            Pattern.compile("ep-\\d+\\s*/?$");

    public List<String> extractEpisodeLinks(String url) throws IOException {
        List<String> results = new ArrayList<>();

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();

        Elements links = doc.select(".eael-grid-post-excerpt a[href]");

        for (Element link : links) {
            String href = link.attr("abs:href").trim();

            if (EP_PATTERN.matcher(href).find()) {
                results.add(href.replaceAll("\\s+/", "/")); // normalize
            }
        }

        return results;
    }
}
