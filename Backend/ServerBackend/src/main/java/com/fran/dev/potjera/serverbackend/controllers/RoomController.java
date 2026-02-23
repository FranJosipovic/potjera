package com.fran.dev.potjera.serverbackend.controllers;

import com.fran.dev.potjera.potjeradb.models.User;
import com.fran.dev.potjera.serverbackend.models.room.CreateRoomRequest;
import com.fran.dev.potjera.serverbackend.models.room.CreateRoomResponse;
import com.fran.dev.potjera.serverbackend.services.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}