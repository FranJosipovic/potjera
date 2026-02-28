package com.fran.dev.potjera.serverbackend.controllers;

import com.fran.dev.potjera.potjeradb.models.User;
import com.fran.dev.potjera.serverbackend.models.room.AssignHunterRequest;
import com.fran.dev.potjera.serverbackend.models.room.CreateRoomRequest;
import com.fran.dev.potjera.serverbackend.models.room.CreateRoomResponse;
import com.fran.dev.potjera.serverbackend.models.room.RoomDetailsResponse;
import com.fran.dev.potjera.serverbackend.services.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<CreateRoomResponse> createRoom(
            @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails  // from JWT
    ) {
        User user = (User) userDetails;
        CreateRoomResponse response = roomService.createRoom(user.getId(), request.isPrivate());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/join/public/{roomId}")
    public ResponseEntity<CreateRoomResponse> joinPublicRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(roomService.joinPublicRoom(roomId, user));
    }

    @PostMapping("/join/private/{code}")
    public ResponseEntity<CreateRoomResponse> joinPrivateRoom(
            @PathVariable String code,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(roomService.joinPrivateRoom(code, user));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDetailsResponse> getRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(roomService.getRoomDetails(roomId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<RoomDetailsResponse>> getPublicRooms() {
        return ResponseEntity.ok(roomService.getPublicRooms());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<RoomDetailsResponse> getRoomByCode(@PathVariable String code) {
        return ResponseEntity.ok(roomService.getRoomByCode(code));
    }

    @PostMapping("/{roomId}/start")
    public ResponseEntity<Void> startGame(
            @PathVariable String roomId,
            @AuthenticationPrincipal User user
    ) {
        roomService.startGame(roomId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/assign-hunter")
    public ResponseEntity<Void> assignHunter(
            @PathVariable String roomId,
            @RequestBody AssignHunterRequest request,  // ← receives {"hunterId": 123}
            @AuthenticationPrincipal User user
    ){
        roomService.assignHunter(roomId, request.hunterId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal User user
    ) {
        roomService.leaveRoom(roomId, user);
        return ResponseEntity.ok().build();
    }
}
