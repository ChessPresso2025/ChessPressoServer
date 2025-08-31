package org.example.chesspressoserver.controller;

import org.example.chesspressoserver.models.GameEntity;
import org.example.chesspressoserver.models.MoveEntity;
import org.example.chesspressoserver.service.GameHistoryService;
import org.example.chesspressoserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GameRestControllerTest {
    private MockMvc mockMvc;
    @Mock
    private GameHistoryService gameHistoryService;
    @Mock
    private UserService userService;

    @InjectMocks
    private GameRestController gameRestController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(gameRestController).build();
        Mockito.when(userService.getUserByUsername(any(String.class))).thenReturn(java.util.Optional.empty());
    }

    @Test
    void getLast10GamesWithMoves_returnsGamesWithMoves() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        GameEntity game = new GameEntity();
        game.setId(gameId);
        game.setWhitePlayerId(userId);
        game.setBlackPlayerId(otherUserId);
        game.setStartedAt(OffsetDateTime.now());
        game.setEndedAt(OffsetDateTime.now());
        game.setResult("1-0");

        MoveEntity move = new MoveEntity();
        move.setId(UUID.randomUUID());
        move.setGame(game);
        move.setMoveNumber(1);
        move.setMoveNotation("e4");
        move.setCreatedAt(OffsetDateTime.now());
        game.setMoves(List.of(move));

        // Mock userService so dass ein User mit userId zurückgegeben wird
        org.example.chesspressoserver.models.User user = new org.example.chesspressoserver.models.User();
        user.setId(userId);
        Mockito.when(userService.getUserByUsername(userId.toString())).thenReturn(java.util.Optional.of(user));

        Mockito.when(gameHistoryService.getLast10GamesWithMoves(userId)).thenReturn(List.of(game));

        mockMvc.perform(get("/api/games/history/" + userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(gameId.toString()))
                .andExpect(jsonPath("$[0].moves[0].moveNotation").value("e4"));
    }

    @Test
    void getLast10GamesWithMoves_returnsEmptyListIfNoGames() throws Exception {
        UUID userId = UUID.randomUUID();
        Mockito.when(gameHistoryService.getLast10GamesWithMoves(any(UUID.class))).thenReturn(List.of());
        mockMvc.perform(get("/api/games/history/" + userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
