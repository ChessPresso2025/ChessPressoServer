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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class GameWebSocketController {
    private static final String ANONYMOUS = "anonymous";
    private static final String PLAYER_ID = "playerId";

    private final OnlinePlayerService onlinePlayerService;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = LoggerFactory.getLogger(GameWebSocketController.class);

    public GameWebSocketController(OnlinePlayerService onlinePlayerService, SimpMessagingTemplate messagingTemplate) {
        this.onlinePlayerService = onlinePlayerService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload Map<String, Object> heartbeatData, Principal principal) {
        String playerId = null;

        // Versuche Player-ID aus dem Principal zu holen
        if (principal != null && !principal.getName().equals(ANONYMOUS)) {
            playerId = principal.getName();
            logger.info("Heartbeat from authenticated user: {}", playerId);
        }

        // Fallback: Versuche Player-ID aus der Nachricht zu holen
        if (playerId == null && heartbeatData != null && heartbeatData.containsKey(PLAYER_ID)) {
            playerId = (String) heartbeatData.get(PLAYER_ID);
            logger.info("Heartbeat from message payload, playerId: {}", playerId);
        }

        if (playerId != null && !playerId.equals(ANONYMOUS)) {
            onlinePlayerService.updateHeartbeat(playerId);
            logger.info("Updated heartbeat for player: {}", playerId);
        } else {
            logger.warn("Heartbeat received but no valid playerId found");
        }
    }

    @MessageMapping("/connect")
    @SendTo("/topic/players")
    public String handlePlayerConnect(Principal principal) {
        String playerId = principal != null ? principal.getName() : ANONYMOUS;
        onlinePlayerService.updateHeartbeat(playerId);
        return "Player " + playerId + " connected";
    }

    @MessageMapping("/players")
    public void getOnlinePlayers(Principal principal) {
        Set<String> onlinePlayers = onlinePlayerService.getOnlinePlayers();
        String playerId = principal != null ? principal.getName() : ANONYMOUS;
        messagingTemplate.convertAndSendToUser(playerId, "/queue/players", onlinePlayers);
    }

    @MessageMapping("/game/join")
    @SendTo("/topic/game")
    public String handleGameJoin(Principal principal) {
        String playerId = principal != null ? principal.getName() : ANONYMOUS;
        onlinePlayerService.updateHeartbeat(playerId);
        return "Player " + playerId + " wants to join game";
    }

    @MessageMapping("/app-closing")
    public void handleAppClosing(@Payload Map<String, Object> closingData, Principal principal) {
        String playerId = null;
        String reason = null;

        // Versuche Player-ID aus dem Principal zu holen
        if (principal != null && !principal.getName().equals(ANONYMOUS)) {
            playerId = principal.getName();
        }

        // Fallback: Versuche Player-ID aus der Nachricht zu holen
        if (playerId == null && closingData != null && closingData.containsKey(PLAYER_ID)) {
            playerId = (String) closingData.get(PLAYER_ID);
        }

        // Extrahiere Grund für das Schließen
        if (closingData != null && closingData.containsKey("reason")) {
            reason = (String) closingData.get("reason");
        }

        if (playerId != null && !playerId.equals(ANONYMOUS)) {
            onlinePlayerService.removePlayer(playerId);
            logger.info("App closing - Player {} removed from online list. Reason: {}", playerId, reason);

            // Sofortige Aktualisierung der Online-Spieler-Liste mit Fehlerbehandlung
            try {
                Set<String> onlinePlayers = onlinePlayerService.getOnlinePlayers();
                Map<String, Object> updateMessage = Map.of(
                        "type", "players-update",
                        "onlinePlayers", onlinePlayers,
                        "playerCount", onlinePlayers.size()
                );
                messagingTemplate.convertAndSend("/topic/players", updateMessage);

                logger.info("Server-Status nach App-Schließung - {} Clients online: {}", onlinePlayers.size(), onlinePlayers);
            } catch (Exception e) {
                logger.error("Error sending update after app closing: {}", e.getMessage());
            }
        } else {
            logger.warn("App closing message received but no valid playerId found");
        }
    }

    @MessageMapping("/disconnect")
    public void handleDisconnect(@Payload Map<String, Object> disconnectData, Principal principal) {
        // Verwende die gleiche Logik wie bei app-closing
        handleAppClosing(disconnectData, principal);
    }
}