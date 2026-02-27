package com.fran.dev.potjera.potjeradb.repositories;

import com.fran.dev.potjera.potjeradb.models.QuickFireQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuickFireQuestionRepository extends JpaRepository<QuickFireQuestion, Long> {
    List<QuickFireQuestion> findByDifficultyBetween(Double min, Double max);

    @Query(value = "SELECT * FROM quick_fire_question ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<QuickFireQuestion> findRandomQuestions(@Param("limit") int limit);
}
