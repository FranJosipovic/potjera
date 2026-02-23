package com.fran.dev.potjera.serverbackend.models.user;

import com.fran.dev.potjera.potjeradb.models.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getDisplayUsername(), // actual "Player_123" username
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
