package com.fran.dev.potjera.potjeradb.repositories;

import com.fran.dev.potjera.potjeradb.enums.RoomStatus;
import com.fran.dev.potjera.potjeradb.models.playmode.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, String> {
    Optional<RoomPlayer> findByRoomIdAndPlayerId(String roomId, Long playerId);
    boolean existsByRoomIdAndPlayerId(String roomId, Long playerId);
    @Query("SELECT COUNT(rp) > 0 FROM RoomPlayer rp WHERE rp.player.id = :playerId AND rp.room.status <> :status")
    boolean existsByPlayerIdAndRoomStatusNot(Long playerId, RoomStatus status);
}
