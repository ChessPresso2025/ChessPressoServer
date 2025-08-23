package org.example.chesspressoserver.components;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlayerCleanupTask {

    private final OnlinePlayerService onlinePlayerService;

    public PlayerCleanupTask(OnlinePlayerService onlinePlayerService) {
        this.onlinePlayerService = onlinePlayerService;
    }

    @Scheduled(fixedRate = 40000) // Alle 40 Sekunden - schnellere Cleanup-Frequenz
    public void cleanupInactivePlayers() {
        onlinePlayerService.cleanup();
    }
}