package com.fran.dev.potjera.serverbackend.controllers;

import com.fran.dev.potjera.potjeradb.repositories.RefreshTokenRepository;
import com.fran.dev.potjera.serverbackend.models.user.AuthResponse;
import com.fran.dev.potjera.serverbackend.models.user.LoginRequest;
import com.fran.dev.potjera.serverbackend.models.user.SignupRequest;
import com.fran.dev.potjera.serverbackend.services.AuthService;
import com.fran.dev.potjera.serverbackend.services.RefreshTokenService;
import com.fran.dev.potjera.serverbackend.utilities.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> payload) {
        String requestToken = payload.get("refreshToken");
        return refreshTokenRepository.findByToken(requestToken)
                .map(token -> {
                    if (refreshTokenService.isTokenExpired(token)) {
                        refreshTokenRepository.delete(token);
                        return ResponseEntity.badRequest().body("Refresh token expired. Please login again.");
                    }
                    String newJwt = jwtUtil.generateToken(token.getUser().getEmail());
                    return ResponseEntity.ok(Map.of("token", newJwt));
                })
                .orElse(ResponseEntity.badRequest().body("Invalid refresh token."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestBody Map<String, String> payload) {
        String requestToken = payload.get("refreshToken");

        if (requestToken == null || requestToken.isBlank()) {
            return ResponseEntity.badRequest().body("Refresh token is required.");
        }

        return refreshTokenRepository.findByToken(requestToken)
                .map(token -> {
                    refreshTokenRepository.delete(token);
                    return ResponseEntity.ok("Logged out successfully.");
                })
                .orElse(ResponseEntity.badRequest().body("Invalid refresh token."));
    }

    @PostMapping("/check-status")
    public ResponseEntity<?> checkStatus(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");
        String token = payload.get("token");
        if (refreshToken == null || refreshToken.isBlank() || token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("Refresh token and token are required.");
        }

        boolean isTokenValid = jwtUtil.isTokenValid(token);

        var dbRefreshToken = refreshTokenRepository.findByToken(refreshToken);

        if (dbRefreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid refresh token.");
        }

        if (isTokenValid) {
            return ResponseEntity.ok(Map.of("token", token, "refreshToken", refreshToken));
        }

        return dbRefreshToken
                .map(refToken -> {
                    boolean isRefreshTokenValid = refreshTokenService.isTokenExpired(refToken);
                    if (isRefreshTokenValid) {
                        String newJwt = jwtUtil.generateToken(refToken.getUser().getEmail());
                        return ResponseEntity.ok(Map.of("token", newJwt, "refreshToken", refToken.getToken()));
                    }
                    refreshTokenRepository.delete(refToken);
                    return ResponseEntity.badRequest().body("Invalid refresh token.");
                })
                .orElse(ResponseEntity.badRequest().body("Invalid refresh token."));
    }
}
