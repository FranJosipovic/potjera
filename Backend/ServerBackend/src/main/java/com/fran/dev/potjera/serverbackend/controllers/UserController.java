package com.fran.dev.potjera.serverbackend.controllers;

import com.fran.dev.potjera.potjeradb.models.User;
import com.fran.dev.potjera.serverbackend.models.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
