package org.example.chesspressoserver.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chesspressoserver.models.GameTime;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.JwtService;
import org.example.chesspressoserver.service.UserService;
import org.example.chesspressoserver.dto.ChatMessage;
import org.example.chesspressoserver.dto.ReadyMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Controller
public class LobbyWebSocketController {

    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtService jwtService;
    private final UserService userService;
    private final LobbyWebSocketManager lobbyManager;

    public LobbyWebSocketController(LobbyService lobbyService,
                                  SimpMessagingTemplate messagingTemplate,
                                  JwtService jwtService,
                                  UserService userService,
                                  LobbyWebSocketManager lobbyManager) {
        this.lobbyService = lobbyService;
        this.messagingTemplate = messagingTemplate;
        this.jwtService = jwtService;
        this.userService = userService;
        this.lobbyManager = lobbyManager;
    }

    /**
     * Handler für Lobby-Beitritt - registriert Verbindung und sendet isolierte Updates
     */
    @MessageMapping("/lobby/join")
    public void handleLobbyJoin(@Payload LobbyJoinMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String username = extractUsernameOrSendError(message.getPlayerId(), headerAccessor, "Lobby-Beitritt");
        if (username == null) return;
        var lobby = validateLobbyAndMembership(message.getLobbyId(), message.getPlayerId(), false);
        if (lobby == null) return;

        try {
            // Registriere WebSocket-Verbindung für diese spezifische Lobby
            lobbyManager.registerLobbyConnection(message.getLobbyId(), message.getPlayerId());

            // Sende Lobby-Status-Update nur an diese Lobby (nicht an andere!)
            lobbyManager.sendLobbyStatusUpdate(message.getLobbyId());

            // Zusätzliche Willkommensnachricht nur für diese Lobby
            lobbyManager.broadcastToLobby(message.getLobbyId(), "PLAYER_JOINED", Map.of(
                "playerId", message.getPlayerId(),
                "username", username,
                "message", username + " ist der Lobby beigetreten",
                "totalPlayers", ((org.example.chesspressoserver.models.Lobby)lobby).getPlayers().size()
            ));
        } catch (Exception e) {
            sendErrorToUser(message.getPlayerId(), "Fehler beim Lobby-Beitritt: " + e.getMessage());
        }
    }

    /**
     * Handler für Lobby-Verlassen - entfernt Verbindung aus spezifischer Lobby
     */
    @MessageMapping("/lobby/leave")
    public void handleLobbyLeave(@Payload LobbyLeaveMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String username = extractUsernameOrSendError(message.getPlayerId(), headerAccessor, "Lobby-Verlassen");
        if (username == null) return;
        try {
            lobbyManager.unregisterLobbyConnection(message.getLobbyId(), message.getPlayerId());
            lobbyManager.broadcastToLobby(message.getLobbyId(), "PLAYER_LEFT", Map.of(
                "playerId", message.getPlayerId(),
                "username", username,
                "message", username + " hat die Lobby verlassen"
            ));
        } catch (Exception e) {
            // Stille Behandlung beim Verlassen
        }
    }

    /**
     * Erweiterte Chat-Message Handler mit Lobby-Isolation
     */
    @MessageMapping("/lobby/chat")
    public void handleLobbyChat(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String username = extractUsernameOrSendError(message.getPlayerId(), headerAccessor, "Chat");
        if (username == null) return;
        var lobby = validateLobbyAndMembership(message.getLobbyId(), message.getPlayerId(), true);
        if (lobby == null) return;
        try {
            lobbyManager.broadcastToLobby(message.getLobbyId(), "CHAT_MESSAGE", Map.of(
                "playerId", message.getPlayerId(),
                "username", username,
                "message", message.getMessage(),
                "timestamp", message.getTimestamp() != null ? message.getTimestamp() : System.currentTimeMillis()
            ));
        } catch (Exception e) {
            sendErrorToUser(message.getPlayerId(), "Fehler beim Chat: " + e.getMessage());
        }
    }

    /**
     * Erweiterte Player-Ready Handler mit Lobby-Isolation
     */
    @MessageMapping("/lobby/ready")
    public void handlePlayerReady(@Payload ReadyMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String username = extractUsernameOrSendError(message.getPlayerId(), headerAccessor, "Ready-Status");
        if (username == null) return;
        var lobby = validateLobbyAndMembership(message.getLobbyId(), message.getPlayerId(), true);
        if (lobby == null) return;
        try {
            boolean success = lobbyService.updatePlayerReadyStatus(message.getLobbyId(), message.getPlayerId(), message.isReady());
            if (success) {
                lobbyManager.broadcastToLobby(message.getLobbyId(), "PLAYER_READY_UPDATE", Map.of(
                    "playerId", message.getPlayerId(),
                    "username", username,
                    "ready", message.isReady(),
                    "message", username + (message.isReady() ? " ist bereit" : " ist nicht mehr bereit")
                ));
                lobbyManager.sendLobbyStatusUpdate(message.getLobbyId());
                if (lobbyService.areAllPlayersReady(message.getLobbyId()) && ((org.example.chesspressoserver.models.Lobby)lobby).getPlayers().size() >= 2) {
                    lobbyManager.broadcastToLobby(message.getLobbyId(), "GAME_STARTING", Map.of(
                        "message", "Alle Spieler sind bereit! Spiel startet...",
                        "players", ((org.example.chesspressoserver.models.Lobby)lobby).getPlayers().stream()
                            .map(playerId -> Map.of("playerId", playerId, "username", userService.getUsernameById(playerId)))
                            .toList(),
                        "gameTime", ((org.example.chesspressoserver.models.Lobby)lobby).getGameTime()
                    ));
                    lobbyService.startGameIfReady(message.getLobbyId());
                }
            } else {
                sendErrorToUser(message.getPlayerId(), "Konnte Ready-Status nicht aktualisieren");
            }
        } catch (Exception e) {
            sendErrorToUser(message.getPlayerId(), "Fehler beim Ready-Status: " + e.getMessage());
        }
    }

    /**
     * Debug-Endpoint um Lobby-Isolation zu testen
     */
    @MessageMapping("/lobby/test-isolation")
    public void testLobbyIsolation(@Payload Map<String, String> request, SimpMessageHeaderAccessor headerAccessor) {
        String playerId = request.getOrDefault("playerId", "");
        String username = extractUsernameOrSendError(playerId, headerAccessor, "Isolation-Test");
        if (username == null) return;
        String lobbyId = request.get("lobbyId");
        if (lobbyId != null) {
            try {
                lobbyManager.broadcastToLobby(lobbyId, "ISOLATION_TEST", Map.of(
                    "message", "Isolation-Test von " + username,
                    "lobbyId", lobbyId,
                    "activeLobbies", lobbyManager.getActiveLobbies().size(),
                    "connectionsInThisLobby", lobbyManager.getActiveConnectionsCount(lobbyId)
                ));
            } catch (Exception e) {
                // Stille Behandlung
            }
        }
    }

    /**
     * Handler für explizites Schließen einer Lobby durch den Client
     */
    @MessageMapping("/lobby/close")
    public void handleLobbyClose(@Payload org.example.chesspressoserver.dto.LobbyCloseMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String username = extractUsernameOrSendError(message.getPlayerId(), headerAccessor, "Lobby-Schließen");
        if (username == null) return;
        var lobby = lobbyService.getLobby(message.getLobbyId());
        if (lobby == null) {
            sendErrorToUser(message.getPlayerId(), "Lobby nicht gefunden");
            return;
        }
        // Optional: Nur Creator darf schließen
        if (!lobby.getCreator().equals(message.getPlayerId())) {
            sendErrorToUser(message.getPlayerId(), "Nur der Creator darf die Lobby schließen");
            return;
        }
        lobbyService.closeLobby(message.getLobbyId());
    }

    // --- Utility methods to reduce code duplication ---
    /**
     * Extrahiert das Token aus den Session-Attributen und gibt den Username zurück.
     * Sendet bei Fehler eine Nachricht an den User und gibt null zurück.
     */
    private String extractUsernameOrSendError(String playerId, SimpMessageHeaderAccessor headerAccessor, String errorPrefix) {
        String token = (String) headerAccessor.getSessionAttributes().get("token");
        if (token == null) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", errorPrefix + ": Authentifizierung erforderlich"));
            return null;
        }
        try {
            return jwtService.getUsernameFromToken(token);
        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", errorPrefix + ": Fehler bei Authentifizierung: " + e.getMessage()));
            return null;
        }
    }

    /**
     * Validiert, ob die Lobby existiert und der Spieler Mitglied ist. Sendet ggf. Fehler.
     * Gibt das Lobby-Objekt zurück oder null bei Fehler.
     */
    private Object validateLobbyAndMembership(String lobbyId, String playerId, boolean checkMembership) {
        var lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Lobby nicht gefunden", "lobbyId", lobbyId));
            return null;
        }
        if (checkMembership && !lobby.getPlayers().contains(playerId)) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Du bist nicht in dieser Lobby", "lobbyId", lobbyId));
            return null;
        }
        return lobby;
    }

    /**
     * Sendet eine Fehlernachricht an den User
     */
    private void sendErrorToUser(String playerId, String message) {
        messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error", Map.of("error", message));
    }

    // Message DTOs
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigureLobbyMessage {
        private String lobbyCode;
        private GameTime gameTime;
        private String whitePlayer;
        private String blackPlayer;
        private boolean randomColors;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LobbyMessage {
        private String content;
        private String sender;
        private String timestamp;
        private String messageType; // CHAT, SYSTEM, etc.
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameMoveMessage {
        private String from;
        private String to;
        private String piece;
        private String playerId;
        private String timestamp;
        private String moveNotation; // z.B. "e2-e4"
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerReadyMessage {
        private String lobbyId;
        private boolean ready;
    }

    // Neue Message DTOs für erweiterte Funktionalität
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LobbyJoinMessage {
        private String lobbyId;
        private String playerId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerInfo {
        private String playerId;
        private String username;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LobbyUpdateMessage {
        private String lobbyId;
        private List<PlayerInfo> players;
        private String status;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnhancedChatMessage {
        private String lobbyId;
        private String playerId;
        private String username;
        private String message;
        private Long timestamp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnhancedReadyMessage {
        private String lobbyId;
        private String playerId;
        private String username;
        private boolean ready;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameStartMessage {
        private String lobbyId;
        private List<PlayerInfo> players;
        private String whitePlayer;
        private String blackPlayer;
        private GameTime gameTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LobbyLeaveMessage {
        private String lobbyId;
        private String playerId;
    }
}
