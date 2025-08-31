package org.example.chesspressoserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chesspressoserver.dto.GameHistoryDto;
import org.example.chesspressoserver.dto.MoveDto;
import org.example.chesspressoserver.dto.StatsReportRequest;
import org.example.chesspressoserver.dto.StatsResponse;
import org.example.chesspressoserver.models.GameEntity;
import org.example.chesspressoserver.models.MoveEntity;
import org.example.chesspressoserver.service.GameHistoryService;
import org.example.chesspressoserver.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final StatsService statsService;
    private final GameHistoryService gameHistoryService;

    @PostMapping("/report")
    public ResponseEntity<Void> reportGameResult(@Valid @RequestBody StatsReportRequest request,
                                                Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        statsService.reportGameResult(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<StatsResponse> getMyStats(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        StatsResponse stats = statsService.getUserStats(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/history")
    public ResponseEntity<List<GameHistoryDto>> getLast10GamesWithMoves(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<GameEntity> games = gameHistoryService.getLast10GamesWithMoves(userId);
        List<GameHistoryDto> result = new ArrayList<>();
        for (GameEntity game : games) {
            GameHistoryDto dto = new GameHistoryDto();
            dto.id = game.getId();
            dto.startedAt = game.getStartedAt();
            dto.endedAt = game.getEndedAt();
            dto.result = game.getResult();
            dto.moves = new ArrayList<>();
            if (game.getMoves() != null) {
                for (MoveEntity move : game.getMoves()) {
                    MoveDto m = new MoveDto();
                    m.id = move.getId();
                    m.moveNumber = move.getMoveNumber();
                    m.moveNotation = move.getMoveNotation();
                    m.createdAt = move.getCreatedAt();
                    dto.moves.add(m);
                }
            }
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }
}
