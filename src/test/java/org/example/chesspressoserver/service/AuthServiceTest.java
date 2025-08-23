package org.example.chesspressoserver.service;

import org.example.chesspressoserver.dto.LoginRequest;
import org.example.chesspressoserver.dto.RegisterRequest;
import org.example.chesspressoserver.dto.TokenResponse;
import org.example.chesspressoserver.models.User;
import org.example.chesspressoserver.models.UserStats;
import org.example.chesspressoserver.repository.UserRepository;
import org.example.chesspressoserver.repository.UserStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setLogin("testuser");
        loginRequest.setPassword("password123");

        mockUser = new User("testuser", "test@example.com", "encodedPassword");
        mockUser.setId(UUID.randomUUID());
    }

    @Test
    void register_ShouldCreateUserAndReturnToken() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userStatsRepository.save(any(UserStats.class))).thenReturn(new UserStats());
        when(jwtService.generateToken(any(UUID.class), anyString())).thenReturn("test-token");

        // When
        TokenResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("test-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    void register_ShouldThrowExceptionWhenUsernameExists() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Username bereits vergeben");
    }

    @Test
    void register_ShouldThrowExceptionWhenEmailExists() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email bereits vergeben");
    }

    @Test
    void login_ShouldReturnTokenForValidCredentials() {
        // Given
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(mockUser.getId(), "testuser")).thenReturn("test-token");
        when(jwtService.getExpirationInSeconds()).thenReturn(3600L);

        // When
        TokenResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("test-token");
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    void login_ShouldThrowExceptionForInvalidUsername() {
        // Given
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ungültige Anmeldedaten");
    }

    @Test
    void login_ShouldThrowExceptionForInvalidPassword() {
        // Given
        when(userRepository.findByUsernameOrEmail("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ungültige Anmeldedaten");
    }
}
