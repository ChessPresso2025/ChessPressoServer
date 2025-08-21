package org.example.chesspressoserver.components;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

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
        broadcastPlayerUpdate();
    }

    // Neue Methode für sofortige Updates bei Connect/Disconnect
    public void broadcastPlayerUpdate() {
        Set<String> onlinePlayers = onlinePlayerService.getOnlinePlayers();
        List<String> failedPlayers = new ArrayList<>();

        System.out.println("Server-Status an " + onlinePlayers.size() + " Clients gesendet: " + onlinePlayers);

        // Sende die Liste aller Online-Spieler an jeden Online-Spieler
        for (String playerId : onlinePlayers) {
            try {
                Map<String, Object> connectionMessage = Map.of(
                        "type", "connection-status",
                        "status", "online",
                        "onlinePlayers", onlinePlayers,
                        "playerCount", onlinePlayers.size()
                );

                simpMessagingTemplate.convertAndSendToUser(
                        playerId, "/queue/status", connectionMessage
                );
            } catch (Exception e) {
                System.out.println("Failed to send message to player " + playerId + ": " + e.getMessage());
                failedPlayers.add(playerId);
            }
        }

        // Entferne Spieler mit fehlgeschlagenen Verbindungen
        for (String failedPlayer : failedPlayers) {
            onlinePlayerService.removePlayer(failedPlayer);
            System.out.println("Removed player with failed connection: " + failedPlayer);
        }

        // Zusätzlich: Sende auch an das öffentliche Topic für alle Subscriber
        try {
            Map<String, Object> publicMessage = Map.of(
                    "type", "players-update",
                    "onlinePlayers", onlinePlayerService.getOnlinePlayers(), // Aktualisierte Liste nach Bereinigung
                    "playerCount", onlinePlayerService.getOnlinePlayers().size()
            );

            simpMessagingTemplate.convertAndSend("/topic/players", publicMessage);
        } catch (Exception e) {
            System.out.println("Failed to send public message: " + e.getMessage());
        }
    }
}
