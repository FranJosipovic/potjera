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
import com.fran.dev.potjera.serverbackend.models.room.websocket.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        if (alreadyInRoom) {
            throw new IllegalStateException("User is already in an active room");
        }

        Room room = Room.builder()
                .status(RoomStatus.WAITING)
                .maxPlayers(5)
                .code(generateUniqueCode())
                .isPrivate(isPrivate)
                .host(host)
                .build();

        roomRepository.save(room);

        RoomPlayer hostPlayer = RoomPlayer.builder()
                .room(room)
                .player(host)
                .isHost(true)
                .isReady(true)
                .isHunter(false)
                .isCaptain(false)
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

        return processJoin(room, user);
    }

    @Transactional
    public CreateRoomResponse joinPrivateRoom(String roomId, String code, User user) {
        Room room = roomRepository.findByIdWithPlayers(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        if (!code.equals(room.getCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid room code");
        }

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

        RoomPlayer roomPlayer = RoomPlayer.builder()
                .room(room)
                .player(user)
                .isHost(false)
                .isReady(true)
                .isHunter(false)
                .isCaptain(false)
                .build();

        roomPlayer.setHunter(room.getPlayers().size() == room.getMaxPlayers() - 1);

        roomPlayerRepository.save(roomPlayer);

        messagingTemplate.convertAndSend(
                "/topic/room/" + room.getId(),
                new RoomEvent("PLAYER_JOINED", new PlayerJoinedPayload(
                        roomPlayer.getId(),
                        user.getId(),
                        user.getDisplayUsername(),
                        roomPlayer.isHunter(),
                        roomPlayer.isReady(),
                        roomPlayer.isCaptain(),
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
                        rp.isHunter(),
                        rp.isCaptain()
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

    // Service
    public RoomDetailsResponse searchRoom(String roomName) {
        return roomRepository.findPrivateWaitingRoomsByHostName(roomName)
                .stream()
                .map(this::mapToRoomDetails)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Room not found for: " + roomName));
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
                        rp.isHunter(),
                        rp.isCaptain()
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

    @Transactional
    public void assignHunter(String roomId, Long hunterId, User user) {
        Room room = roomRepository.findByIdWithPlayers(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        boolean isHost = room.getPlayers().stream()
                .anyMatch(rp -> rp.getPlayer().getId().equals(user.getId()) && rp.isHost());

        if (!isHost) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room host can assign a hunter");
        }

        boolean playerInRoom = room.getPlayers().stream()
                .anyMatch(rp -> rp.getPlayer().getId().equals(hunterId));

        if (!playerInRoom) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player is not in this room");
        }

        // clear current hunter
        room.getPlayers().forEach(rp -> {
            rp.setHunter(false);
            roomPlayerRepository.save(rp);
        });

        // assign new hunter
        RoomPlayer newHunter = room.getPlayers().stream()
                .filter(rp -> rp.getPlayer().getId().equals(hunterId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Player not found in room"));

        newHunter.setHunter(true);
        roomPlayerRepository.save(newHunter);

        // broadcast hunter changed
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                new RoomEvent("HUNTER_CHANGED", new HunterChangedPayload(
                        newHunter.getId(),
                        newHunter.getPlayer().getId()
                ))
        );
    }

    @Transactional
    public void assignCaptain(String roomId, Long captainId, User user) {
        Room room = roomRepository.findByIdWithPlayers(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        boolean isHost = room.getPlayers().stream()
                .anyMatch(rp -> rp.getPlayer().getId().equals(user.getId()) && rp.isHost());

        if (!isHost) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room host can assign a hunter");
        }

        boolean playerInRoom = room.getPlayers().stream()
                .anyMatch(rp -> rp.getPlayer().getId().equals(captainId));

        if (!playerInRoom) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player is not in this room");
        }

        // clear current captain
        room.getPlayers().forEach(rp -> {
            rp.setCaptain(false);
            roomPlayerRepository.save(rp);
        });

        // assign new captain
        RoomPlayer newCaptain = room.getPlayers().stream()
                .filter(rp -> rp.getPlayer().getId().equals(captainId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Player not found in room"));

        newCaptain.setCaptain(true);
        roomPlayerRepository.save(newCaptain);

        // broadcast hunter changed
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                new RoomEvent("CAPTAIN_CHANGED", new CaptainChangedPayload(
                        newCaptain.getId(),
                        newCaptain.getPlayer().getId()
                ))
        );
    }

    @Transactional
    public void leaveRoom(String roomId, User user) {
        Room room = roomRepository.findByIdWithPlayers(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        RoomPlayer leavingPlayer = room.getPlayers().stream()
                .filter(rp -> rp.getPlayer().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "You are not in this room"
                ));

        boolean wasHunter = leavingPlayer.isHunter();
        boolean wasHost = leavingPlayer.isHost();
        boolean wasCaptain = leavingPlayer.isCaptain();

        // if host leaves → close room and notify everyone
        if (wasHost) {
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    new RoomEvent("ROOM_CLOSED", Map.of(
                            "reason", "Host left the room"
                    ))
            );
            roomRepository.delete(room);
            return;
        }

        roomPlayerRepository.delete(leavingPlayer);
        room.getPlayers().remove(leavingPlayer);

        if (room.getPlayers().isEmpty()) {
            roomRepository.delete(room);
            return;
        }

        Long newHunterId = null;
        Long newCaptainId = null;

        // assign new hunter if hunter left
        if (wasHunter) {
            room.getPlayers().forEach(rp -> {
                rp.setHunter(false);
                roomPlayerRepository.save(rp);
            });

            RoomPlayer newHunter = room.getPlayers().getLast();
            newHunter.setHunter(true);
            roomPlayerRepository.save(newHunter);
            newHunterId = newHunter.getPlayer().getId();
        }

        if (wasCaptain) {
            room.getPlayers().forEach(rp -> {
                rp.setCaptain(false);
                roomPlayerRepository.save(rp);
            });

            Optional<RoomPlayer> newCaptainOptional = room.getPlayers().stream().filter(p -> !p.isHunter()).findAny();
            if (newCaptainOptional.isPresent()) {
                var newCaptain = newCaptainOptional.get();
                newCaptain.setCaptain(true);
                roomPlayerRepository.save(newCaptain);
                newCaptainId = newCaptain.getPlayer().getId();
            }
        }

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                new RoomEvent("PLAYER_LEFT", new PlayerLeftRoomPayload(
                        user.getId(),    // no new host since room closes if host leaves
                        newHunterId,
                        newCaptainId
                ))
        );
    }
}
