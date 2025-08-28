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
        String token = (String) headerAccessor.getSessionAttributes().get("token");

        if (token == null) {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Authentifizierung erforderlich"));
            return;
        }

        try {
            String username = jwtService.getUsernameFromToken(token);
            var lobby = lobbyService.getLobby(message.getLobbyId());

            if (lobby == null) {
                messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                    Map.of("error", "Lobby nicht gefunden", "lobbyId", message.getLobbyId()));
                return;
            }

            // Registriere WebSocket-Verbindung für diese spezifische Lobby
            lobbyManager.registerLobbyConnection(message.getLobbyId(), message.getPlayerId());

            // Sende Lobby-Status-Update nur an diese Lobby (nicht an andere!)
            lobbyManager.sendLobbyStatusUpdate(message.getLobbyId());

            // Zusätzliche Willkommensnachricht nur für diese Lobby
            lobbyManager.broadcastToLobby(message.getLobbyId(), "PLAYER_JOINED", Map.of(
                "playerId", message.getPlayerId(),
                "username", username,
                "message", username + " ist der Lobby beigetreten",
                "totalPlayers", lobby.getPlayers().size()
            ));

        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Fehler beim Lobby-Beitritt: " + e.getMessage()));
        }
    }

    /**
     * Handler für Lobby-Verlassen - entfernt Verbindung aus spezifischer Lobby
     */
    @MessageMapping("/lobby/leave")
    public void handleLobbyLeave(@Payload LobbyLeaveMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String token = (String) headerAccessor.getSessionAttributes().get("token");

        if (token != null) {
            try {
                String username = jwtService.getUsernameFromToken(token);

                // Entferne WebSocket-Verbindung nur aus dieser spezifischen Lobby
                lobbyManager.unregisterLobbyConnection(message.getLobbyId(), message.getPlayerId());

                // Benachrichtige nur die verbleibenden Spieler in DIESER Lobby
                lobbyManager.broadcastToLobby(message.getLobbyId(), "PLAYER_LEFT", Map.of(
                    "playerId", message.getPlayerId(),
                    "username", username,
                    "message", username + " hat die Lobby verlassen"
                ));

            } catch (Exception e) {
                // Stille Behandlung beim Verlassen
            }
        }
    }

    /**
     * Erweiterte Chat-Message Handler mit Lobby-Isolation
     */
    @MessageMapping("/lobby/chat")
    public void handleLobbyChat(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String token = (String) headerAccessor.getSessionAttributes().get("token");

        if (token == null) {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Authentifizierung erforderlich"));
            return;
        }

        try {
            String username = jwtService.getUsernameFromToken(token);

            // Validiere Lobby-Existenz
            var lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) {
                messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                    Map.of("error", "Lobby nicht gefunden", "lobbyId", message.getLobbyId()));
                return;
            }

            // Prüfe ob Spieler in der Lobby ist
            if (!lobby.getPlayers().contains(message.getPlayerId())) {
                messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                    Map.of("error", "Du bist nicht in dieser Lobby", "lobbyId", message.getLobbyId()));
                return;
            }

            // Chat-Nachricht wird NUR an diese spezifische Lobby gesendet
            lobbyManager.broadcastToLobby(message.getLobbyId(), "CHAT_MESSAGE", Map.of(
                "playerId", message.getPlayerId(),
                "username", username,
                "message", message.getMessage(),
                "timestamp", message.getTimestamp() != null ? message.getTimestamp() : System.currentTimeMillis()
            ));

        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Fehler beim Chat: " + e.getMessage()));
        }
    }

    /**
     * Erweiterte Player-Ready Handler mit Lobby-Isolation
     */
    @MessageMapping("/lobby/ready")
    public void handlePlayerReady(@Payload ReadyMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String token = (String) headerAccessor.getSessionAttributes().get("token");

        if (token == null) {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Authentifizierung erforderlich"));
            return;
        }

        try {
            String username = jwtService.getUsernameFromToken(token);

            // Validiere Lobby-Existenz
            var lobby = lobbyService.getLobby(message.getLobbyId());
            if (lobby == null) {
                messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                    Map.of("error", "Lobby nicht gefunden", "lobbyId", message.getLobbyId()));
                return;
            }

            // Prüfe ob Spieler in der Lobby ist
            if (!lobby.getPlayers().contains(message.getPlayerId())) {
                messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                    Map.of("error", "Du bist nicht in dieser Lobby", "lobbyId", message.getLobbyId()));
                return;
            }

            // Update Ready-Status im LobbyService
            boolean success = lobbyService.updatePlayerReadyStatus(message.getLobbyId(), message.getPlayerId(), message.isReady());

            if (success) {
                // Ready-Update wird NUR an diese spezifische Lobby gesendet
                lobbyManager.broadcastToLobby(message.getLobbyId(), "PLAYER_READY_UPDATE", Map.of(
                    "playerId", message.getPlayerId(),
                    "username", username,
                    "ready", message.isReady(),
                    "message", username + (message.isReady() ? " ist bereit" : " ist nicht mehr bereit")
                ));

                // Aktualisiere Lobby-Status nur für diese Lobby
                lobbyManager.sendLobbyStatusUpdate(message.getLobbyId());

                // Prüfe ob alle Spieler bereit sind und starte ggf. das Spiel
                if (lobbyService.areAllPlayersReady(message.getLobbyId()) && lobby.getPlayers().size() >= 2) {
                    // Spielstart-Nachricht nur an diese Lobby
                    lobbyManager.broadcastToLobby(message.getLobbyId(), "GAME_STARTING", Map.of(
                        "message", "Alle Spieler sind bereit! Spiel startet...",
                        "players", lobby.getPlayers().stream()
                            .map(playerId -> Map.of("playerId", playerId, "username", userService.getUsernameById(playerId)))
                            .toList(),
                        "gameTime", lobby.getGameTime()
                    ));

                    lobbyService.startGameIfReady(message.getLobbyId());
                }
            } else {
                messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                    Map.of("error", "Konnte Ready-Status nicht aktualisieren", "lobbyId", message.getLobbyId()));
            }

        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Fehler beim Ready-Status: " + e.getMessage()));
        }
    }

    /**
     * Debug-Endpoint um Lobby-Isolation zu testen
     */
    @MessageMapping("/lobby/test-isolation")
    public void testLobbyIsolation(@Payload Map<String, String> request, SimpMessageHeaderAccessor headerAccessor) {
        String token = (String) headerAccessor.getSessionAttributes().get("token");

        if (token != null) {
            try {
                String username = jwtService.getUsernameFromToken(token);
                String lobbyId = request.get("lobbyId");

                if (lobbyId != null) {
                    // Sende Test-Nachricht nur an die angegebene Lobby
                    lobbyManager.broadcastToLobby(lobbyId, "ISOLATION_TEST", Map.of(
                        "message", "Isolation-Test von " + username,
                        "lobbyId", lobbyId,
                        "activeLobbies", lobbyManager.getActiveLobbies().size(),
                        "connectionsInThisLobby", lobbyManager.getActiveConnectionsCount(lobbyId)
                    ));
                }
            } catch (Exception e) {
                // Stille Behandlung
            }
        }
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
