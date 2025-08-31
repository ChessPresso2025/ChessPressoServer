package org.example.chesspressoserver.service;

import org.example.chesspressoserver.dto.StatsReportRequest;
import org.example.chesspressoserver.dto.StatsResponse;
import org.example.chesspressoserver.models.UserStats;
import org.example.chesspressoserver.repository.UserStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private UserStatsRepository userStatsRepository;

    @InjectMocks
    private StatsService statsService;

    private UUID userId;
    private UserStats mockUserStats;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        mockUserStats = UserStats.builder()
                .userId(userId)
                .wins(10)
                .losses(5)
                .draws(2)
                .build();
    }

    @Test
    void getUserStats_ShouldReturnStatsForExistingUser() {
        // Given
        when(userStatsRepository.findById(userId)).thenReturn(Optional.of(mockUserStats));

        // When
        StatsResponse response = statsService.getUserStats(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getWins()).isEqualTo(10);
        assertThat(response.getLosses()).isEqualTo(5);
        assertThat(response.getDraws()).isEqualTo(2);
        assertThat(response.getTotal()).isEqualTo(17);
    }

    @Test
    void getUserStats_ShouldReturnDefaultStatsForNonExistentUser() {
        // Given
        when(userStatsRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        StatsResponse response = statsService.getUserStats(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getWins()).isEqualTo(0);
        assertThat(response.getLosses()).isEqualTo(0);
        assertThat(response.getDraws()).isEqualTo(0);
        assertThat(response.getTotal()).isEqualTo(0);
    }

    @Test
    void reportGameResult_ShouldUpdateWinsForWinResult() {
        // Given
        StatsReportRequest request = new StatsReportRequest();
        request.setResult("WIN");

        when(userStatsRepository.existsById(userId)).thenReturn(true);

        // When
        statsService.reportGameResult(userId, request);

        // Then
        verify(userStatsRepository).incrementWins(userId);
    }

    @Test
    void reportGameResult_ShouldUpdateLossesForLossResult() {
        // Given
        StatsReportRequest request = new StatsReportRequest();
        request.setResult("LOSS");

        when(userStatsRepository.existsById(userId)).thenReturn(true);

        // When
        statsService.reportGameResult(userId, request);

        // Then
        verify(userStatsRepository).incrementLosses(userId);
    }

    @Test
    void reportGameResult_ShouldUpdateDrawsForDrawResult() {
        // Given
        StatsReportRequest request = new StatsReportRequest();
        request.setResult("DRAW");

        when(userStatsRepository.existsById(userId)).thenReturn(true);

        // When
        statsService.reportGameResult(userId, request);

        // Then
        verify(userStatsRepository).incrementDraws(userId);
    }

    @Test
    void reportGameResult_ShouldCreateStatsIfNotExist() {
        // Given
        StatsReportRequest request = new StatsReportRequest();
        request.setResult("WIN");

        when(userStatsRepository.existsById(userId)).thenReturn(false);

        // When
        statsService.reportGameResult(userId, request);

        // Then
        verify(userStatsRepository).save(any(UserStats.class));
        verify(userStatsRepository).incrementWins(userId);
    }
}
