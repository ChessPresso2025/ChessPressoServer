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
    void startGame_success_sendsWebSocketMessage() throws Exception {
        String lobbyId = "lobby123";
        doNothing().when(gameManager).startGame(eq(lobbyId));

        StartGameRequest request = new StartGameRequest();
        request.setLobbyId(lobbyId);
        request.setPlayerId("host");

        mockMvc.perform(post("/game/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(lobbyId)));

        verify(gameManager).startGame(eq(lobbyId));
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/lobby/" + lobbyId), mapCaptor.capture());
        Map<String, Object> msg = mapCaptor.getValue();
        assert msg.get("type").equals("gameStarted");
        assert msg.get("lobbyId").equals(lobbyId);
    }

    @Test
    void startGame_missingLobbyId_returnsBadRequest() throws Exception {
        StartGameRequest request = new StartGameRequest();
        request.setPlayerId("host");
        mockMvc.perform(post("/game/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lobby-ID fehlt")));
        verifyNoInteractions(gameManager, messagingTemplate);
    }

    @Test
    void resignGame_success() throws Exception {
        String lobbyId = "lobby123";
        when(gameManager.resignGame(eq(lobbyId))).thenReturn(true);
        ResignGameRequest request = new ResignGameRequest();
        request.setLobbyId(lobbyId);
        request.setPlayerId("p1");
        mockMvc.perform(post("/game/resign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("aufgegeben")));
        verify(gameManager).resignGame(eq(lobbyId));
    }

    @Test
    void resignGame_missingLobbyId_returnsBadRequest() throws Exception {
        ResignGameRequest request = new ResignGameRequest();
        request.setPlayerId("p1");
        mockMvc.perform(post("/game/resign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lobby-ID fehlt")));
        verifyNoInteractions(gameManager);
    }

    @Test
    void resignGame_invalidLobby_returnsBadRequest() throws Exception {
        String lobbyId = "lobby123";
        when(gameManager.resignGame(eq(lobbyId))).thenReturn(false);
        ResignGameRequest request = new ResignGameRequest();
        request.setLobbyId(lobbyId);
        request.setPlayerId("p1");
        mockMvc.perform(post("/game/resign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ungültige Lobby-ID")));
        verify(gameManager).resignGame(eq(lobbyId));
    }

    @Test
    void rematch_success() throws Exception {
        String lobbyId = "lobby123";
        when(gameManager.rematch(eq(lobbyId))).thenReturn(true);
        RematchRequest request = new RematchRequest();
        request.setLobbyId(lobbyId);
        request.setPlayerId("p1");
        mockMvc.perform(post("/game/rematch")
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
        mockMvc.perform(post("/game/rematch")
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
        mockMvc.perform(post("/game/rematch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ungültige Lobby-ID")));
        verify(gameManager).rematch(eq(lobbyId));
    }
}
