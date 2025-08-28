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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Controller
public class LobbyWebSocketController {

    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtService jwtService;
    private final UserService userService;

    public LobbyWebSocketController(LobbyService lobbyService, SimpMessagingTemplate messagingTemplate, JwtService jwtService, UserService userService) {
        this.lobbyService = lobbyService;
        this.messagingTemplate = messagingTemplate;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * Handler für Lobby-Beitritt - sendet Spielerinformationen mit dekodierten Benutzernamen
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

            // Erstelle Spielerinformationen mit dekodierten Benutzernamen
            List<PlayerInfo> playersInfo = new ArrayList<>();
            for (String playerId : lobby.getPlayers()) {
                String playerUsername = getUsernameForPlayer(playerId);
                playersInfo.add(new PlayerInfo(playerId, playerUsername));
            }

            // Sende Lobby-Update mit Spielerinformationen an alle Teilnehmer
            LobbyUpdateMessage updateMessage = new LobbyUpdateMessage(
                message.getLobbyId(),
                playersInfo,
                lobby.getStatus().toString(),
                "Spieler " + username + " ist der Lobby beigetreten"
            );

            messagingTemplate.convertAndSend("/topic/lobby/" + message.getLobbyId(), updateMessage);

        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Fehler beim Lobby-Beitritt: " + e.getMessage()));
        }
    }

    /**
     * Erweiterte Chat-Message Handler mit dekodiertem Benutzernamen
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

            // Setze Timestamp falls nicht vorhanden
            if (message.getTimestamp() == null) {
                message.setTimestamp(System.currentTimeMillis());
            }

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

            // Erweitere Chat-Nachricht um dekodierte Benutzername
            EnhancedChatMessage enhancedMessage = new EnhancedChatMessage(
                message.getLobbyId(),
                message.getPlayerId(),
                username,
                message.getMessage(),
                message.getTimestamp()
            );

            // Broadcast Chat-Nachricht an alle Lobby-Teilnehmer
            messagingTemplate.convertAndSend("/topic/lobby/" + message.getLobbyId(), enhancedMessage);

        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Fehler beim Chat: " + e.getMessage()));
        }
    }

    /**
     * Erweiterte Player-Ready Handler mit dekodiertem Benutzernamen
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
                // Erweiterte Ready-Nachricht mit dekodiertem Benutzernamen
                EnhancedReadyMessage enhancedMessage = new EnhancedReadyMessage(
                    message.getLobbyId(),
                    message.getPlayerId(),
                    username,
                    message.isReady()
                );

                // Broadcast Ready-Update an alle Lobby-Teilnehmer
                messagingTemplate.convertAndSend("/topic/lobby/" + message.getLobbyId(), enhancedMessage);

                // Prüfe ob alle Spieler bereit sind und starte ggf. das Spiel
                if (lobbyService.areAllPlayersReady(message.getLobbyId()) && lobby.getPlayers().size() >= 2) {
                    // Sende Spielstart-Nachricht mit Spielerinformationen
                    sendGameStartMessage(message.getLobbyId(), lobby);
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
     * Sendet Spielstart-Nachricht mit Spielerinformationen
     */
    private void sendGameStartMessage(String lobbyId, org.example.chesspressoserver.models.Lobby lobby) {
        List<PlayerInfo> playersInfo = new ArrayList<>();
        for (String playerId : lobby.getPlayers()) {
            String username = getUsernameForPlayer(playerId);
            playersInfo.add(new PlayerInfo(playerId, username));
        }

        GameStartMessage gameStartMessage = new GameStartMessage(
            lobbyId,
            playersInfo,
            lobby.getWhitePlayer(),
            lobby.getBlackPlayer(),
            lobby.getGameTime()
        );

        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, gameStartMessage);
    }

    /**
     * Hilfsmethode zum Abrufen des Benutzernamens für einen Spieler
     */
    private String getUsernameForPlayer(String playerId) {
        // Hier würden Sie normalerweise den Benutzernamen aus einer Datenbank oder Cache abrufen
        // Für jetzt geben wir den playerId zurück, falls der Username nicht verfügbar ist
        try {
            // Implementierung würde hier den tatsächlichen Username aus der Datenbank abrufen
            return userService.getUsernameById(playerId);
        } catch (Exception e) {
            return playerId; // Fallback bei Fehlern
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
}
