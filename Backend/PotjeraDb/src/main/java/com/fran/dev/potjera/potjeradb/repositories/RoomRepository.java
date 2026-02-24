package com.fran.dev.potjera.potjeradb.repositories;

import com.fran.dev.potjera.potjeradb.models.playmode.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findByCode(String code);
    boolean existsByCode(String code);

    @Query("SELECT r FROM Room r JOIN FETCH r.players rp JOIN FETCH rp.player WHERE r.id = :roomId")
    Optional<Room> findByIdWithPlayers(@Param("roomId") String roomId);

    // public rooms = code is null, status is WAITING
    @Query("SELECT r FROM Room r JOIN FETCH r.players rp JOIN FETCH rp.player WHERE r.code IS NULL AND r.status = 'WAITING'")
    List<Room> findPublicWaitingRooms();

    @Query("SELECT r FROM Room r JOIN FETCH r.players rp JOIN FETCH rp.player WHERE r.code = :code")
    Optional<Room> findByCodeWithPlayers(@Param("code") String code);
}
