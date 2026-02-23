package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.potjeradb.enums.RoomStatus;
import com.fran.dev.potjera.potjeradb.models.User;
import com.fran.dev.potjera.potjeradb.models.playmode.Room;
import com.fran.dev.potjera.potjeradb.models.playmode.RoomPlayer;
import com.fran.dev.potjera.potjeradb.repositories.RoomPlayerRepository;
import com.fran.dev.potjera.potjeradb.repositories.RoomRepository;
import com.fran.dev.potjera.potjeradb.repositories.UserRepository;
import com.fran.dev.potjera.serverbackend.models.room.CreateRoomResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateRoomResponse createRoom(Long hostId, boolean isPrivate) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // check if user is already in an active room
        boolean alreadyInRoom = roomPlayerRepository.existsByPlayerIdAndRoomStatusNot(
                hostId, RoomStatus.FINISHED
        );
        if (alreadyInRoom) {
            throw new IllegalStateException("User is already in an active room");
        }

        Room room = Room.builder()
                .status(RoomStatus.WAITING)
                .maxPlayers(5)
                .code(isPrivate ? generateUniqueCode() : null)
                .build();

        roomRepository.save(room);

        RoomPlayer hostPlayer = RoomPlayer.builder()
                .room(room)
                .player(host)
                .isHost(true)
                .isReady(false)
                .isHunter(false)
                .build();

        roomPlayerRepository.save(hostPlayer);

        return new CreateRoomResponse(
                room.getId(),
                room.getCode(),
                room.getStatus(),
                room.getMaxPlayers(),
                room.getCreatedAt()
        );
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generateCode();
        } while (roomRepository.existsByCode(code));
        return code;
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}
