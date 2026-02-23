/*
package com.fran.dev.potjera.potjeradb.models.playmode;

import com.fran.dev.potjera.potjeradb.enums.CoinBoosterStatus;
import com.fran.dev.potjera.potjeradb.enums.GameStage;
import com.fran.dev.potjera.potjeradb.models.QuickFireQuestion;
import com.fran.dev.potjera.potjeradb.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity@Table(name = "coin_booster_game_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinBoosterGameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSession gameSession;

    @OneToOne
    @JoinColumn(name = "player_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private CoinBoosterStatus status;

    private int correctAnswers = 0;

    @OneToMany
    private List<QuickFireQuestion> questions;
}
*/
