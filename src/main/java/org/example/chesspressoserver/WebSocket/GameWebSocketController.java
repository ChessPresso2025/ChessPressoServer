package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

@Controller
public class GameWebSocketController {

    private final OnlinePlayerService onlinePlayerService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(OnlinePlayerService onlinePlayerService, SimpMessagingTemplate messagingTemplate) {
        this.onlinePlayerService = onlinePlayerService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload Map<String, Object> heartbeatData, Principal principal) {
        String playerId = null;

        // Versuche Player-ID aus dem Principal zu holen
        if (principal != null && !principal.getName().equals("anonymous")) {
            playerId = principal.getName();
            System.out.println("Heartbeat from authenticated user: " + playerId);
        }

        // Fallback: Versuche Player-ID aus der Nachricht zu holen
        if (playerId == null && heartbeatData != null && heartbeatData.containsKey("playerId")) {
            playerId = (String) heartbeatData.get("playerId");
            System.out.println("Heartbeat from message payload, playerId: " + playerId);
        }

        if (playerId != null && !playerId.equals("anonymous")) {
            onlinePlayerService.updateHeartbeat(playerId);
            System.out.println("Updated heartbeat for player: " + playerId);
        } else {
            System.out.println("Heartbeat received but no valid playerId found");
        }
    }

    @MessageMapping("/connect")
    @SendTo("/topic/players")
    public String handlePlayerConnect(Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";
        onlinePlayerService.updateHeartbeat(playerId);
        return "Player " + playerId + " connected";
    }

    @MessageMapping("/players")
    public void getOnlinePlayers(Principal principal) {
        Set<String> onlinePlayers = onlinePlayerService.getOnlinePlayers();
        String playerId = principal != null ? principal.getName() : "anonymous";
        messagingTemplate.convertAndSendToUser(playerId, "/queue/players", onlinePlayers);
    }

    @MessageMapping("/game/join")
    @SendTo("/topic/game")
    public String handleGameJoin(Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";
        onlinePlayerService.updateHeartbeat(playerId);
        return "Player " + playerId + " wants to join game";
    }

    @MessageMapping("/app-closing")
    public void handleAppClosing(@Payload Map<String, Object> closingData, Principal principal) {
        String playerId = null;
        String reason = null;

        // Versuche Player-ID aus dem Principal zu holen
        if (principal != null && !principal.getName().equals("anonymous")) {
            playerId = principal.getName();
        }

        // Fallback: Versuche Player-ID aus der Nachricht zu holen
        if (playerId == null && closingData != null && closingData.containsKey("playerId")) {
            playerId = (String) closingData.get("playerId");
        }

        // Extrahiere Grund für das Schließen
        if (closingData != null && closingData.containsKey("reason")) {
            reason = (String) closingData.get("reason");
        }

        if (playerId != null && !playerId.equals("anonymous")) {
            onlinePlayerService.removePlayer(playerId);
            System.out.println("App closing - Player " + playerId + " removed from online list. Reason: " + reason);

            // Sofortige Aktualisierung der Online-Spieler-Liste mit Fehlerbehandlung
            try {
                Set<String> onlinePlayers = onlinePlayerService.getOnlinePlayers();
                Map<String, Object> updateMessage = Map.of(
                        "type", "players-update",
                        "onlinePlayers", onlinePlayers,
                        "playerCount", onlinePlayers.size()
                );
                messagingTemplate.convertAndSend("/topic/players", updateMessage);

                System.out.println("Server-Status nach App-Schließung - " + onlinePlayers.size() + " Clients online: " + onlinePlayers);
            } catch (Exception e) {
                System.out.println("Error sending update after app closing: " + e.getMessage());
            }
        } else {
            System.out.println("App closing message received but no valid playerId found");
        }
    }

    @MessageMapping("/disconnect")
    public void handleDisconnect(@Payload Map<String, Object> disconnectData, Principal principal) {
        // Verwende die gleiche Logik wie bei app-closing
        handleAppClosing(disconnectData, principal);
    }
}