/*
package com.fran.dev.potjera.potjeradb.models.playmode;

import com.fran.dev.potjera.potjeradb.enums.PlayersTurnGameSessionStatus;
import com.fran.dev.potjera.potjeradb.models.QuickFireQuestion;
import com.fran.dev.potjera.potjeradb.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "players_turn_game_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayersTurnGameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "players", nullable = false)
    private List<User> users;

    @Enumerated(EnumType.STRING)
    private PlayersTurnGameSessionStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answering_user_id", nullable = false)
    private User answeringUser;

    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;

    @OneToMany
    private List<QuickFireQuestion> questions;
}
*/
