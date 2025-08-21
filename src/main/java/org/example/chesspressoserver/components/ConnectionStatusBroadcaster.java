package org.example.chesspressoserver.components;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ConnectionStatusBroadcaster {
    private final OnlinePlayerService onlinePlayerService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public ConnectionStatusBroadcaster(SimpMessagingTemplate simpMessagingTemplate, OnlinePlayerService onlinePlayerService) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.onlinePlayerService = onlinePlayerService;
    }

    @Scheduled(fixedRate = 5000) // Alle 5 Sekunden - korrigierte Frequenz
    public void broadcastConnectionStatus() {
        Set<String> onlinePlayers = onlinePlayerService.getOnlinePlayers();

        System.out.println("Server-Status an " + onlinePlayers.size() + " Clients gesendet: " + onlinePlayers);

        // Sende die Liste aller Online-Spieler an jeden Online-Spieler
        for (String playerId : onlinePlayers) {
            Map<String, Object> connectionMessage = Map.of(
                    "type", "connection-status",
                    "status", "online",
                    "onlinePlayers", onlinePlayers,
                    "playerCount", onlinePlayers.size()
            );

            simpMessagingTemplate.convertAndSendToUser(
                    playerId, "/queue/status", connectionMessage
            );
        }

        // Zusätzlich: Sende auch an das öffentliche Topic für alle Subscriber
        if (!onlinePlayers.isEmpty()) {
            Map<String, Object> publicMessage = Map.of(
                    "type", "players-update",
                    "onlinePlayers", onlinePlayers,
                    "playerCount", onlinePlayers.size()
            );

            simpMessagingTemplate.convertAndSend("/topic/players", publicMessage);
        }
    }
}
