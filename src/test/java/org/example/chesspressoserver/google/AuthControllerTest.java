package org.example.chesspressoserver.google;

import org.example.chesspressoserver.models.Player;
import org.example.chesspressoserver.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @Test
    void loginReturnsUserDataOnValidCredentials() throws Exception {
        // Mock Player mit allen notwendigen Eigenschaften
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getPlayerId()).thenReturn("player-123");
        when(mockPlayer.getName()).thenReturn("testuser");
        when(mockPlayer.getPlayedGames()).thenReturn(5);
        when(mockPlayer.getWin()).thenReturn(3);
        when(mockPlayer.getDraw()).thenReturn(1);
        when(mockPlayer.getLose()).thenReturn(1);
        when(mockPlayer.getEmail()).thenReturn("test@example.com");

        when(playerService.authenticatePlayer("testuser", "password123")).thenReturn(mockPlayer);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("player-123"))
                .andExpect(jsonPath("$.name").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.playedGames").value(5))
                .andExpect(jsonPath("$.win").value(3))
                .andExpect(jsonPath("$.draw").value(1))
                .andExpect(jsonPath("$.lose").value(1));
    }

    @Test
    void loginReturnsUnauthorizedOnInvalidCredentials() throws Exception {
        when(playerService.authenticatePlayer("testuser", "wrongpassword")).thenReturn(null);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerReturnsUserDataOnValidInput() throws Exception {
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getPlayerId()).thenReturn("player-456");
        when(mockPlayer.getName()).thenReturn("newuser");
        when(mockPlayer.getPlayedGames()).thenReturn(0);
        when(mockPlayer.getWin()).thenReturn(0);
        when(mockPlayer.getDraw()).thenReturn(0);
        when(mockPlayer.getLose()).thenReturn(0);
        when(mockPlayer.getEmail()).thenReturn("new@example.com");

        when(playerService.registerPlayer("newuser", "password123", "new@example.com")).thenReturn(mockPlayer);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"password\":\"password123\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("player-456"))
                .andExpect(jsonPath("$.name").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void registerReturnsConflictOnDuplicateUsername() throws Exception {
        when(playerService.registerPlayer("existinguser", "password123", "test@example.com"))
                .thenThrow(new IllegalArgumentException("Benutzername bereits vergeben"));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"existinguser\",\"password\":\"password123\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isConflict());
    }
}