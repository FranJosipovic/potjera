package com.fran.dev.potjera.serverbackend.services;

import com.fran.dev.potjera.potjeradb.models.RefreshToken;
import com.fran.dev.potjera.serverbackend.models.user.AuthResponse;
import com.fran.dev.potjera.serverbackend.models.user.LoginRequest;
import com.fran.dev.potjera.serverbackend.models.user.SignupRequest;
import com.fran.dev.potjera.serverbackend.utilities.JwtUtil;
import com.fran.dev.potjera.potjeradb.models.User;
import com.fran.dev.potjera.potjeradb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return new AuthResponse(token, refreshToken.getToken(), user.getUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        // This throws AuthenticationException if credentials are wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return new AuthResponse(token, refreshToken.getToken(), user.getUsername(), user.getEmail());
    }
}
