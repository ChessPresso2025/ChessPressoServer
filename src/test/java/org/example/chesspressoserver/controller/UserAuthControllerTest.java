package org.example.chesspressoserver.controller;

import org.example.chesspressoserver.dto.LoginRequest;
import org.example.chesspressoserver.dto.RegisterRequest;
import org.example.chesspressoserver.dto.TokenResponse;
import org.example.chesspressoserver.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserAuthController userAuthController;

    @Test
    void register_ShouldReturnTokenResponse() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        TokenResponse tokenResponse = new TokenResponse("test-token", 3600L, "testuser");
        when(authService.register(any(RegisterRequest.class))).thenReturn(tokenResponse);

        // When
        ResponseEntity<TokenResponse> response = userAuthController.register(request);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("test-token");
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
    }

    @Test
    void login_ShouldReturnTokenResponse() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setLogin("testuser");
        request.setPassword("password123");

        TokenResponse tokenResponse = new TokenResponse("test-token", 3600L, "testuser");
        when(authService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        // When
        ResponseEntity<TokenResponse> response = userAuthController.login(request);

        // Then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isEqualTo("test-token");
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
    }
}
