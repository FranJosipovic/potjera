package com.fran.dev.potjera.potjeradb.repositories;

import com.fran.dev.potjera.potjeradb.models.MultipleChoiceQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultipleChoiceQuestionRepository extends JpaRepository<MultipleChoiceQuestion, Long> {
    List<MultipleChoiceQuestion> findByDifficultyBetween(Double min, Double max);

    @Query(value = "SELECT * FROM multiple_choice_question ORDER BY RANDOM() LIMIT :limit",
            nativeQuery = true)
    List<MultipleChoiceQuestion> findRandomQuestions(@Param("limit") int limit);

    @Query(value = """
            SELECT * FROM multiple_choice_question
            WHERE question NOT IN (:usedQuestions)
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<MultipleChoiceQuestion> findRandomQuestionsExcluding(
            @Param("limit") int limit,
            @Param("usedQuestions") List<String> usedQuestions
    );
}
