package org.example.chesspressoserver.service;

import org.example.chesspressoserver.dto.LoginRequest;
import org.example.chesspressoserver.dto.RegisterRequest;
import org.example.chesspressoserver.dto.TokenResponse;
import org.example.chesspressoserver.models.User;
import org.example.chesspressoserver.models.UserStats;
import org.example.chesspressoserver.repository.UserRepository;
import org.example.chesspressoserver.repository.UserStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    public AuthService(UserRepository userRepository, UserStatsRepository userStatsRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        // Check for existing username or email
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username bereits vergeben");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email bereits vergeben");
        }

        try {
            // Create new user
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .build();

            user = userRepository.save(user);

            // Create initial stats for user (simplified without user reference)
            UserStats stats = UserStats.builder()
                    .userId(user.getId())
                    .wins(0)
                    .losses(0)
                    .draws(0)
                    .build();

            userStatsRepository.save(stats);

            // Generate token
            String token = jwtService.generateToken(user.getId(), user.getUsername());

            return new TokenResponse(
                    token,
                    jwtService.getExpirationInSeconds(),
                    user.getUsername()
            );

        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation during registration: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username oder Email bereits vergeben");
        }
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.getLogin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ungültige Anmeldedaten"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ungültige Anmeldedaten");
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());

        return new TokenResponse(
                token,
                jwtService.getExpirationInSeconds(),
                user.getUsername()
        );
    }
}
