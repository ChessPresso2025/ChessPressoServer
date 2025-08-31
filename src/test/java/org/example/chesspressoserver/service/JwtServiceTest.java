package org.example.chesspressoserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String testSecret = "test-secret-key-that-is-long-enough-for-jwt-signing";
        long testExpiration = 3600000; // 1 hour

        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", testExpiration);
        jwtService.init();
    }

    @Test
    void generateToken_ShouldGenerateValidToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String username = "testuser";

        // When
        String token = jwtService.generateToken(userId, username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts separated by dots
    }

    @Test
    void getUserIdFromToken_ShouldReturnCorrectUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        String username = "testuser";
        String token = jwtService.generateToken(userId, username);

        // When
        UUID extractedUserId = jwtService.getUserIdFromToken(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        // Given
        UUID userId = UUID.randomUUID();
        String username = "testuser";
        String token = jwtService.generateToken(userId, username);

        // When
        String extractedUsername = jwtService.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String username = "testuser";
        String token = jwtService.generateToken(userId, username);

        // When
        boolean isValid = jwtService.validateToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_ShouldReturnFalseForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtService.validateToken(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void getUserIdFromToken_ShouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When/Then
        assertThatThrownBy(() -> jwtService.getUserIdFromToken(invalidToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void getUsernameFromToken_ShouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When/Then
        assertThatThrownBy(() -> jwtService.getUsernameFromToken(invalidToken))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid JWT token");
    }

    @Test
    void init_ShouldThrowExceptionForEmptySecret() {
        // Given
        JwtService jwtServiceWithEmptySecret = new JwtService();
        ReflectionTestUtils.setField(jwtServiceWithEmptySecret, "jwtSecret", "");

        // When/Then
        assertThatThrownBy(jwtServiceWithEmptySecret::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET environment variable is required");
    }

    @Test
    void init_ShouldThrowExceptionForNullSecret() {
        // Given
        JwtService jwtServiceWithNullSecret = new JwtService();
        ReflectionTestUtils.setField(jwtServiceWithNullSecret, "jwtSecret", null);

        // When/Then
        assertThatThrownBy(jwtServiceWithNullSecret::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET environment variable is required");
    }
}
