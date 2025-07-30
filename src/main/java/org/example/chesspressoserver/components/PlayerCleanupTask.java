package org.example.chesspressoserver.components;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.scheduling.annotation.Scheduled;

public class PlayerCleanupTask {
    private final OnlinePlayerService onlinePlayerService;

    public PlayerCleanupTask(OnlinePlayerService onlinePlayerService) {
        this.onlinePlayerService = onlinePlayerService;
    }

    @Scheduled(fixedRate = 5000)
    public void cleanup() {
        onlinePlayerService.cleanup();
    }
}
