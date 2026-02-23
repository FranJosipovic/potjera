package com.fran.dev.potjera.potjeradb.repositories;

import com.fran.dev.potjera.potjeradb.models.playmode.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, String> {
    Optional<Room> findByCode(String code);
    boolean existsByCode(String code);
}