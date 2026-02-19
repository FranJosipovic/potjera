package com.fran.dev.potjeradb.repositories;

import com.fran.dev.potjeradb.models.QuickFireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuickFireQuestionRepository extends JpaRepository<QuickFireQuestion, Long> {
    List<QuickFireQuestion> findByDifficultyBetween(Double min, Double max);
}
