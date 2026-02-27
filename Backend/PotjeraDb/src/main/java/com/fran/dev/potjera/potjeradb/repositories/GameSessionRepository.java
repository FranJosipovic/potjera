package com.fran.dev.potjera.potjeradb.repositories;

import com.fran.dev.potjera.potjeradb.models.playmode.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, String> {
    Optional<GameSession> findByRoomId(String roomId);
}
