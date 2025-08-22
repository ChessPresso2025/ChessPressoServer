package org.example.chesspressoserver.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chesspressoserver.models.GameTime;
import org.example.chesspressoserver.service.LobbyService;
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

@Controller
public class LobbyWebSocketController {

    private final LobbyService lobbyService;
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyWebSocketController(LobbyService lobbyService, SimpMessagingTemplate messagingTemplate) {
        this.lobbyService = lobbyService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Neuer Chat-Message Handler für Lobby-Chat
     * Destination: /app/lobby/chat
     */
    @MessageMapping("/lobby/chat")
    public void handleLobbyChat(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        // Setze Timestamp falls nicht vorhanden
        if (message.getTimestamp() == null) {
            message.setTimestamp(System.currentTimeMillis());
        }

        // Validiere Lobby-Existenz
        var lobby = lobbyService.getLobby(message.getLobbyId());
        if (lobby == null) {
            // Sende Fehler zurück an Sender
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

        // Broadcast Chat-Nachricht an alle Lobby-Teilnehmer
        messagingTemplate.convertAndSend("/topic/lobby/" + message.getLobbyId(), message);
    }

    /**
     * Neuer Player-Ready Handler
     * Destination: /app/lobby/ready
     */
    @MessageMapping("/lobby/ready")
    public void handlePlayerReady(@Payload ReadyMessage message, SimpMessageHeaderAccessor headerAccessor) {
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
            // Broadcast Ready-Update an alle Lobby-Teilnehmer
            messagingTemplate.convertAndSend("/topic/lobby/" + message.getLobbyId(), message);

            // Prüfe ob alle Spieler bereit sind und starte ggf. das Spiel
            if (lobbyService.areAllPlayersReady(message.getLobbyId()) && lobby.getPlayers().size() >= 2) {
                // Spiel automatisch starten wenn alle bereit sind
                lobbyService.startGameIfReady(message.getLobbyId());
            }
        } else {
            messagingTemplate.convertAndSendToUser(message.getPlayerId(), "/queue/lobby-error",
                Map.of("error", "Konnte Ready-Status nicht aktualisieren", "lobbyId", message.getLobbyId()));
        }
    }

    @MessageMapping("/lobby/configure")
    public void configureLobby(ConfigureLobbyMessage message, Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        boolean success = lobbyService.configurePrivateLobby(
            playerId,
            message.getLobbyCode(),
            message.getGameTime(),
            message.getWhitePlayer(),
            message.getBlackPlayer(),
            message.isRandomColors()
        );

        if (!success) {
            messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error",
                Map.of("error", "Konnte Lobby nicht konfigurieren"));
        }
    }

    @MessageMapping("/lobby/{lobbyId}/message")
    @SendTo("/topic/lobby/{lobbyId}")
    public LobbyMessage sendLobbyMessage(@DestinationVariable String lobbyId,
                                       LobbyMessage message,
                                       Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        // Prüfe ob Spieler in der Lobby ist
        var lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null || !lobby.getPlayers().contains(playerId)) {
            return null; // Nachricht wird nicht gesendet
        }

        message.setSender(playerId);
        message.setTimestamp(LocalDateTime.now().toString());
        return message;
    }

    @MessageMapping("/lobby/{lobbyId}/move")
    @SendTo("/topic/lobby/{lobbyId}")
    public GameMoveMessage sendGameMove(@DestinationVariable String lobbyId,
                                      GameMoveMessage move,
                                      Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        // Prüfe ob Spieler in der Lobby ist und das Spiel läuft
        var lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null || !lobby.getPlayers().contains(playerId) || !lobby.isGameStarted()) {
            return null;
        }

        move.setPlayerId(playerId);
        move.setTimestamp(LocalDateTime.now().toString());
        return move;
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
}
