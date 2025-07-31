package org.example.chesspressoserver.serice;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OnlinePlayerServiceTest {

    private OnlinePlayerService service;

    @BeforeEach
    void setUp() {
        service = new OnlinePlayerService();
    }

    @Test
    void testUpdateHeartbeatStoresTimestamp() {
        service.updateHeartbeat("player1");
        Set<String> online = service.getOnlinePlayers();
        assertTrue(online.contains("player1"));
    }

    @Test
    void testCleanupRemovesExpiredPlayers() throws InterruptedException {
        service.updateHeartbeat("expiredPlayer");
        Thread.sleep(2100); // simulate 2s timeout (adjust timeout in service for real test)
        service.cleanup();
        Set<String> online = service.getOnlinePlayers();
        assertFalse(online.contains("expiredPlayer"));
    }
}