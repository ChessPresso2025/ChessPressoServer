package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.GameEntity;
import org.example.chesspressoserver.models.MoveEntity;
import org.example.chesspressoserver.repository.GameRepository;
import org.example.chesspressoserver.repository.MoveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GameHistoryService {
    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;

    public GameHistoryService(GameRepository gameRepository, MoveRepository moveRepository) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
    }

    public List<GameEntity> getLast10GamesWithMoves(UUID userId) {
        List<GameEntity> games = gameRepository.findTop10ByUserIdOrderByStartedAtDesc(userId);
        for (GameEntity game : games) {
            List<MoveEntity> moves = moveRepository.findByGameIdOrderByMoveNumberAsc(game.getId());
            game.setMoves(moves);
        }
        return games;
    }
}
