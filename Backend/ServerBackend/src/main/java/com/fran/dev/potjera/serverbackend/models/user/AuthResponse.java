package com.fran.dev.potjera.serverbackend.models.user;

import lombok.AllArgsConstructor;
import lombok.Data;

// AuthResponse.java
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String email;
}