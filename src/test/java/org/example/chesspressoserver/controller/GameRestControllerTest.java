package org.example.chesspressoserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chesspressoserver.gamelogic.GameManager;
import org.example.chesspressoserver.models.requests.StartGameRequest;
import org.example.chesspressoserver.models.requests.ResignGameRequest;
import org.example.chesspressoserver.models.requests.RematchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GameRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GameManager gameManager;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private GameRestController gameRestController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        reset(gameManager, messagingTemplate);
        mockMvc = MockMvcBuilders.standaloneSetup(gameRestController).build();
    }

    @Test
    void rematch_success() throws Exception {
        String lobbyId = "lobby123";
        when(gameManager.rematch(eq(lobbyId))).thenReturn(true);
        RematchRequest request = new RematchRequest();
        request.setLobbyId(lobbyId);
        request.setPlayerId("p1");
        mockMvc.perform(post("/rematch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(lobbyId)));
        verify(gameManager).rematch(eq(lobbyId));
    }

    @Test
    void rematch_missingLobbyId_returnsBadRequest() throws Exception {
        RematchRequest request = new RematchRequest();
        request.setPlayerId("p1");
        mockMvc.perform(post("/rematch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lobby-ID fehlt")));
        verifyNoInteractions(gameManager);
    }

    @Test
    void rematch_invalidLobby_returnsBadRequest() throws Exception {
        String lobbyId = "lobby123";
        when(gameManager.rematch(eq(lobbyId))).thenReturn(false);
        RematchRequest request = new RematchRequest();
        request.setLobbyId(lobbyId);
        request.setPlayerId("p1");
        mockMvc.perform(post("/rematch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ungültige Lobby-ID")));
        verify(gameManager).rematch(eq(lobbyId));
    }
}
