package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.GameEntity;
import org.example.chesspressoserver.models.MoveEntity;
import org.example.chesspressoserver.repository.GameRepository;
import org.example.chesspressoserver.repository.MoveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameHistoryServiceTest {
    @Mock
    private GameRepository gameRepository;
    @Mock
    private MoveRepository moveRepository;
    @InjectMocks
    private GameHistoryService gameHistoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getLast10GamesWithMoves_returnsGamesWithMoves() {
        UUID userId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        GameEntity game = new GameEntity();
        game.setId(gameId);
        game.setUserId(userId);
        game.setStartedAt(OffsetDateTime.now());
        game.setEndedAt(OffsetDateTime.now());
        game.setResult("1-0");

        MoveEntity move1 = new MoveEntity();
        move1.setId(UUID.randomUUID());
        move1.setGame(game);
        move1.setMoveNumber(1);
        move1.setMoveNotation("e4");
        move1.setCreatedAt(OffsetDateTime.now());

        MoveEntity move2 = new MoveEntity();
        move2.setId(UUID.randomUUID());
        move2.setGame(game);
        move2.setMoveNumber(2);
        move2.setMoveNotation("e5");
        move2.setCreatedAt(OffsetDateTime.now());

        when(gameRepository.findTop10ByUserIdOrderByStartedAtDesc(userId)).thenReturn(List.of(game));
        when(moveRepository.findByGameIdOrderByMoveNumberAsc(gameId)).thenReturn(List.of(move1, move2));

        List<GameEntity> result = gameHistoryService.getLast10GamesWithMoves(userId);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getMoves().size());
        assertEquals("e4", result.get(0).getMoves().get(0).getMoveNotation());
        assertEquals("e5", result.get(0).getMoves().get(1).getMoveNotation());
    }

    @Test
    void getLast10GamesWithMoves_returnsEmptyListIfNoGames() {
        UUID userId = UUID.randomUUID();
        when(gameRepository.findTop10ByUserIdOrderByStartedAtDesc(userId)).thenReturn(Collections.emptyList());
        List<GameEntity> result = gameHistoryService.getLast10GamesWithMoves(userId);
        assertTrue(result.isEmpty());
    }
}

