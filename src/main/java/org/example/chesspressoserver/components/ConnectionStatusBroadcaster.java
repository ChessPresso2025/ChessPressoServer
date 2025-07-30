package org.example.chesspressoserver.components;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.Set;

public class ConnectionStatusBroadcaster {
    private final OnlinePlayerService onlinePlayerService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ConnectionStatusBroadcaster(SimpMessagingTemplate simpMessagingTemplate, OnlinePlayerService onlinePlayerService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.onlinePlayerService = onlinePlayerService;
    }

    @Scheduled(fixedRate = 5000)
    public void broadcastConnectionStatus() {
        Set<String> players = onlinePlayerService.getOnlinePlayers();
        for (String playerId : players) {
            simpMessagingTemplate.convertAndSendToUser(
                    playerId, "/queue/status", Map.of("type", "connection-status", "status", "online")
            );
        }
    }
}
