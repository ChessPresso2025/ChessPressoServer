package org.example.chesspressoserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chesspressoserver.dto.StatsReportRequest;
import org.example.chesspressoserver.dto.StatsResponse;
import org.example.chesspressoserver.models.UserStats;
import org.example.chesspressoserver.repository.UserStatsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {

    private final UserStatsRepository userStatsRepository;

    @Transactional
    public void reportGameResult(UUID userId, StatsReportRequest request) {
        // Ensure user stats exist
        if (!userStatsRepository.existsById(userId)) {
            UserStats stats = UserStats.builder()
                    .userId(userId)
                    .wins(0)
                    .losses(0)
                    .draws(0)
                    .build();
            userStatsRepository.save(stats);
        }

        switch (request.getResult()) {
            case "WIN" -> userStatsRepository.incrementWins(userId);
            case "LOSS" -> userStatsRepository.incrementLosses(userId);
            case "DRAW" -> userStatsRepository.incrementDraws(userId);
        }

        log.debug("Updated stats for user {} with result {}", userId, request.getResult());
    }

    public StatsResponse getUserStats(UUID userId) {
        UserStats stats = userStatsRepository.findById(userId)
                .orElse(UserStats.builder()
                        .userId(userId)
                        .wins(0)
                        .losses(0)
                        .draws(0)
                        .build());

        return new StatsResponse(stats.getWins(), stats.getLosses(), stats.getDraws());
    }
}
