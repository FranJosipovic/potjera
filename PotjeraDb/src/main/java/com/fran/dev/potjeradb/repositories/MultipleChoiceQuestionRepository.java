package com.fran.dev.potjeradb.repositories;

import com.fran.dev.potjeradb.models.MultipleChoiceQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultipleChoiceQuestionRepository extends JpaRepository<MultipleChoiceQuestion, Long> {
    List<MultipleChoiceQuestion> findByDifficultyBetween(Double min, Double max);
}
