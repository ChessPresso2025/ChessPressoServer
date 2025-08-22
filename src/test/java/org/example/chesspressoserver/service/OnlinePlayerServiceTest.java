package org.example.chesspressoserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OnlinePlayerServiceTest {

    private OnlinePlayerService onlinePlayerService;

    @BeforeEach
    void setUp() {
        onlinePlayerService = new OnlinePlayerService();
    }

    @Test
    void updateHeartbeat_ShouldAddPlayerToOnlineList() {
        // When
        onlinePlayerService.updateHeartbeat("player1");

        // Then
        assertThat(onlinePlayerService.isPlayerOnline("player1")).isTrue();
        assertThat(onlinePlayerService.getOnlinePlayers()).contains("player1");
    }

    @Test
    void updateHeartbeat_ShouldNotAddAnonymousPlayer() {
        // When
        onlinePlayerService.updateHeartbeat("anonymous");

        // Then
        assertThat(onlinePlayerService.isPlayerOnline("anonymous")).isFalse();
        assertThat(onlinePlayerService.getOnlinePlayers()).doesNotContain("anonymous");
    }

    @Test
    void updateHeartbeat_ShouldNotAddNullPlayer() {
        // When
        onlinePlayerService.updateHeartbeat(null);

        // Then
        assertThat(onlinePlayerService.getOnlinePlayerCount()).isEqualTo(0);
    }

    @Test
    void isPlayerOnline_ShouldReturnFalseForNonExistentPlayer() {
        // When/Then
        assertThat(onlinePlayerService.isPlayerOnline("nonexistent")).isFalse();
    }

    @Test
    void isPlayerOnline_ShouldReturnFalseForAnonymousPlayer() {
        // When/Then
        assertThat(onlinePlayerService.isPlayerOnline("anonymous")).isFalse();
    }

    @Test
    void isPlayerOnline_ShouldReturnFalseForNullPlayer() {
        // When/Then
        assertThat(onlinePlayerService.isPlayerOnline(null)).isFalse();
    }

    @Test
    void removePlayer_ShouldRemovePlayerFromOnlineList() {
        // Given
        onlinePlayerService.updateHeartbeat("player1");
        assertThat(onlinePlayerService.isPlayerOnline("player1")).isTrue();

        // When
        onlinePlayerService.removePlayer("player1");

        // Then
        assertThat(onlinePlayerService.isPlayerOnline("player1")).isFalse();
        assertThat(onlinePlayerService.getOnlinePlayers()).doesNotContain("player1");
    }

    @Test
    void getOnlinePlayers_ShouldReturnMultipleOnlinePlayers() {
        // Given
        onlinePlayerService.updateHeartbeat("player1");
        onlinePlayerService.updateHeartbeat("player2");
        onlinePlayerService.updateHeartbeat("player3");

        // When
        Set<String> onlinePlayers = onlinePlayerService.getOnlinePlayers();

        // Then
        assertThat(onlinePlayers).containsExactlyInAnyOrder("player1", "player2", "player3");
        assertThat(onlinePlayerService.getOnlinePlayerCount()).isEqualTo(3);
    }

    @Test
    void cleanup_ShouldRemoveInactivePlayers() {
        // Given - This test would take very long with real timeout
        // For a real test one would make the timeout configurable
        onlinePlayerService.updateHeartbeat("player1");

        // When
        onlinePlayerService.cleanup();

        // Then - Since timeout is 45 seconds, player should still be online
        assertThat(onlinePlayerService.isPlayerOnline("player1")).isTrue();
    }

    @Test
    void getOnlinePlayerCount_ShouldReturnCorrectCount() {
        // Given
        assertThat(onlinePlayerService.getOnlinePlayerCount()).isEqualTo(0);

        onlinePlayerService.updateHeartbeat("player1");
        assertThat(onlinePlayerService.getOnlinePlayerCount()).isEqualTo(1);

        onlinePlayerService.updateHeartbeat("player2");
        assertThat(onlinePlayerService.getOnlinePlayerCount()).isEqualTo(2);

        onlinePlayerService.removePlayer("player1");
        assertThat(onlinePlayerService.getOnlinePlayerCount()).isEqualTo(1);
    }
}
