/*
package com.fran.dev.potjera.potjeradb.models.playmode;

import com.fran.dev.potjera.potjeradb.enums.BoardGameStage;
import com.fran.dev.potjera.potjeradb.models.MultipleChoiceQuestion;
import com.fran.dev.potjera.potjeradb.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "board_game_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardGameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @OneToOne
    @JoinColumn(name = "player_id", nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(name = "hunter_id", nullable = false)
    private User hunter;

    @Enumerated(EnumType.STRING)
    private BoardGameStage currentStage;

    private int baseReward;
    private int lowerReward;
    private int upperReward;

    private int maximumReward;
    private int lowestReward;

    private int currentQuestionIndex = 0;
    private int playerCorrectAnswers = 0;
    private int hunterCorrectAnswers = 0;

    private int playerPosition = 0;
    private int hunterPosition = 0;

    private int playerRequiredAnswers = 0;

    @OneToMany
    private List<MultipleChoiceQuestion> questions;
}*/
