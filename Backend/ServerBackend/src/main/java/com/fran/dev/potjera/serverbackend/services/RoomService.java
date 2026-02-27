package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.potjeradb.enums.RoomStatus;
import com.fran.dev.potjera.potjeradb.models.User;
import com.fran.dev.potjera.potjeradb.models.playmode.Room;
import com.fran.dev.potjera.potjeradb.models.playmode.RoomPlayer;
import com.fran.dev.potjera.potjeradb.repositories.QuickFireQuestionRepository;
import com.fran.dev.potjera.potjeradb.repositories.RoomPlayerRepository;
import com.fran.dev.potjera.potjeradb.repositories.RoomRepository;
import com.fran.dev.potjera.potjeradb.repositories.UserRepository;
import com.fran.dev.potjera.serverbackend.models.room.CreateRoomResponse;
import com.fran.dev.potjera.serverbackend.models.room.RoomDetailsResponse;
import com.fran.dev.potjera.serverbackend.models.room.RoomPlayerDTO;
import com.fran.dev.potjera.serverbackend.models.room.websocket.PlayerJoinedPayload;
import com.fran.dev.potjera.serverbackend.models.room.websocket.RoomEvent;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final UserRepository userRepository;
    private final QuickFireQuestionRepository quickFireQuestionRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameSessionService gameSessionService;

    @Transactional
    public CreateRoomResponse createRoom(Long hostId, boolean isPrivate) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // check if user is already in an active room
        boolean alreadyInRoom = roomPlayerRepository.existsByPlayerIdAndRoomStatusNot(
                hostId, RoomStatus.FINISHED
        );
//        if (alreadyInRoom) {
//            throw new IllegalStateException("User is already in an active room");
//        }

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

    @Transactional
    public CreateRoomResponse joinPublicRoom(String roomId, User user) {
        Room room = roomRepository.findByIdWithPlayers(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        if (room.getCode() != null) {
            throw new IllegalStateException("This is a private room");
        }

        return processJoin(room, user);
    }

    @Transactional
    public CreateRoomResponse joinPrivateRoom(String code, User user) {
        Room room = roomRepository.findByCodeWithPlayers(code)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        return processJoin(room, user);
    }

    // shared join logic
    private CreateRoomResponse processJoin(Room room, User user) {
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new IllegalStateException("Room is not available");
        }

        if (room.getPlayers().size() >= room.getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Room is full");
        }

        boolean alreadyJoined = roomPlayerRepository
                .existsByRoomIdAndPlayerId(room.getId(), user.getId());

        if (alreadyJoined) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in this room");
        }

        // set all existing players isHunter = false
        room.getPlayers().forEach(rp -> {
            rp.setHunter(false);
            roomPlayerRepository.save(rp);
        });

        RoomPlayer roomPlayer = RoomPlayer.builder()
                .room(room)
                .player(user)
                .isHost(false)
                .isReady(false)
                .build();

        roomPlayer.setHunter(room.getPlayers().size() == room.getMaxPlayers() - 1);

        roomPlayerRepository.save(roomPlayer);

        messagingTemplate.convertAndSend(
                "/topic/room/" + room.getId(),
                new RoomEvent("PLAYER_JOINED", new PlayerJoinedPayload(
                        user.getId(),
                        user.getDisplayUsername(),
                        roomPlayer.isHunter(),
                        new Random().nextInt(100) + 1
                ))
        );

        return new CreateRoomResponse(
                room.getId(),
                room.getCode(),
                room.getStatus(),
                room.getMaxPlayers(),
                room.getCreatedAt()
        );
    }

    public RoomDetailsResponse getRoomDetails(String roomId) {
        Room room = roomRepository.findByIdWithPlayers(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        List<RoomPlayerDTO> players = room.getPlayers().stream()
                .map(rp -> new RoomPlayerDTO(
                        rp.getId(),
                        rp.getPlayer().getId(),
                        rp.getPlayer().getDisplayUsername(),
                        //rp.getPlayer().getRank(),
                        new Random().nextInt(100) + 1,  // +1 so range is 1-100, not 0-99
                        rp.isHost(),
                        rp.isReady(),
                        rp.isHunter()
                ))
                .toList();

        RoomPlayerDTO hunter = players.stream()
                .filter(RoomPlayerDTO::isHunter)
                .findFirst()
                .orElse(null);

        return new RoomDetailsResponse(
                room.getId(),
                room.getCode(),
                room.getStatus(),
                room.getMaxPlayers(),
                room.getPlayers().size(),
                room.getCreatedAt(),
                players,
                hunter
        );
    }

    public List<RoomDetailsResponse> getPublicRooms() {
        return roomRepository.findPublicWaitingRooms()
                .stream()
                .map(this::mapToRoomDetails)
                .toList();
    }

    public RoomDetailsResponse getRoomByCode(String code) {
        Room room = roomRepository.findByCodeWithPlayers(code)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        return mapToRoomDetails(room);
    }

    // extract mapping to avoid duplication
    private RoomDetailsResponse mapToRoomDetails(Room room) {
        Random random = new Random();

        List<RoomPlayerDTO> players = room.getPlayers().stream()
                .map(rp -> new RoomPlayerDTO(
                        rp.getId(),
                        rp.getPlayer().getId(),
                        rp.getPlayer().getDisplayUsername(),
                        random.nextInt(100) + 1,
                        rp.isHost(),
                        rp.isReady(),
                        rp.isHunter()
                ))
                .toList();

        RoomPlayerDTO hunter = players.stream()
                .filter(RoomPlayerDTO::isHunter)
                .findFirst()
                .orElse(null);

        return new RoomDetailsResponse(
                room.getId(),
                room.getCode(),
                room.getStatus(),
                room.getMaxPlayers(),
                room.getPlayers().size(),
                room.getCreatedAt(),
                players,
                hunter
        );
    }

    @Transactional
    public void startGame(String roomId, User user) {
        Room room = roomRepository.findByIdWithPlayers(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        // check if sender is host
        boolean isHost = room.getPlayers().stream()
                .anyMatch(rp -> rp.getPlayer().getId().equals(user.getId()) && rp.isHost());

        if (!isHost) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only host can start the game");
        }

        if (room.getStatus() != RoomStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room is not in waiting state");
        }

        if (room.getPlayers().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Need at least 2 players to start");
        }

        // update room status
        room.setStatus(RoomStatus.IN_PROGRESS);
        roomRepository.save(room);

        // start the in-memory game session
        gameSessionService.startGame(room);
    }
}
