package org.example.chesspressoserver.components;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class ConnectionStatusBroadcaster {
    private final OnlinePlayerService onlinePlayerService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ConnectionStatusBroadcaster.class);

    public ConnectionStatusBroadcaster(SimpMessagingTemplate simpMessagingTemplate, OnlinePlayerService onlinePlayerService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.onlinePlayerService = onlinePlayerService;
    }

    @Scheduled(fixedRate = 5000) // Alle 5 Sekunden
    public void broadcastConnectionStatus() {
        broadcastPlayerUpdate();
    }

    // Neue Methode für sofortige Updates bei Connect/Disconnect
    public void broadcastPlayerUpdate() {

        try {
            Map<String, Object> publicMessage = Map.of(
                    "type", "status-update",
                    "status", "online"
            );
            simpMessagingTemplate.convertAndSend("/topic/players", publicMessage);
        } catch (Exception e) {
            logger.error("Failed to send public message: {}", e.getMessage());
        }
        logger.info("Broadcasted players status to players: {}", onlinePlayerService.getOnlinePlayers());
    }
}
