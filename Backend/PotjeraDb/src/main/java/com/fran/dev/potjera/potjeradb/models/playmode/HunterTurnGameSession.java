/*
package com.fran.dev.potjera.potjeradb.models.playmode;

import com.fran.dev.potjera.potjeradb.models.QuickFireQuestion;
import com.fran.dev.potjera.potjeradb.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "hunter_turn_game_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HunterTurnGameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hunter_id", nullable = false)
    private User hunter;

    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;

    @OneToMany
    private List<QuickFireQuestion> questions;
}
*/
