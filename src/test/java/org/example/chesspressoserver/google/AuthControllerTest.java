package org.example.chesspressoserver.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.example.chesspressoserver.models.Player;
import org.example.chesspressoserver.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    @MockitoBean
    private PlayerService playerService;

    @Test
    void returnsUserDataOnValidToken() throws Exception {
        GoogleIdToken.Payload payload = Mockito.mock(GoogleIdToken.Payload.class);
        when(payload.get("email")).thenReturn("test@example.com");
        when(payload.get("name")).thenReturn("Max Testmann");
        when(payload.get("sub")).thenReturn("google-id-123");

        // Mock Player mit allen notwendigen Eigenschaften
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getPlayerId()).thenReturn("google-id-123");
        when(mockPlayer.getName()).thenReturn("Max Testmann");
        when(mockPlayer.getPlayedGames()).thenReturn(0);
        when(mockPlayer.getWin()).thenReturn(0);
        when(mockPlayer.getDraw()).thenReturn(0);
        when(mockPlayer.getLose()).thenReturn(0);

        when(playerService.findOrCreatePlayer("google-id-123", "Max Testmann")).thenReturn(mockPlayer);
        doReturn(Optional.of(payload)).when(googleAuthService).verifyToken("valid-token");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Max Testmann"))
                .andExpect(jsonPath("$.playerId").value("google-id-123"));
    }

    @Test
    void returnsUnauthorizedOnInvalidToken() throws Exception {
        when(googleAuthService.verifyToken("invalid-token")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"invalid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid ID token"));
    }

    @Test
    void testAlternativeGoogleTokenEndpoint() throws Exception {
        // Test für alternatives Token Format über HTTP Endpoint
        String alternativeToken = "google_account_104988664958231654178_michael.krametter@gmail.com";

        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.get("email")).thenReturn("michael.krametter@gmail.com");
        when(payload.get("name")).thenReturn("michael.krametter");
        when(payload.get("sub")).thenReturn("104988664958231654178");

        // Mock Player mit allen notwendigen Eigenschaften
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getPlayerId()).thenReturn("104988664958231654178");
        when(mockPlayer.getName()).thenReturn("michael.krametter");
        when(mockPlayer.getPlayedGames()).thenReturn(0);
        when(mockPlayer.getWin()).thenReturn(0);
        when(mockPlayer.getDraw()).thenReturn(0);
        when(mockPlayer.getLose()).thenReturn(0);

        when(playerService.findOrCreatePlayer("104988664958231654178", "michael.krametter")).thenReturn(mockPlayer);

        doReturn(Optional.of(payload)).when(googleAuthService).verifyToken(alternativeToken);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\": \"" + alternativeToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("michael.krametter@gmail.com"))
                .andExpect(jsonPath("$.name").value("michael.krametter"))
                .andExpect(jsonPath("$.playerId").value("104988664958231654178"));
    }
}