package org.example.chesspressoserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chesspressoserver.dto.LoginRequest;
import org.example.chesspressoserver.dto.RegisterRequest;
import org.example.chesspressoserver.dto.TokenResponse;
import org.example.chesspressoserver.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class UserAuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for username: {}", request.getUsername());
        TokenResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for: {}", request.getLogin());
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
