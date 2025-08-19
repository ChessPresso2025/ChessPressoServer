package org.example.chesspressoserver.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chesspressoserver.models.GameTime;
import org.example.chesspressoserver.service.LobbyService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
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

    //für spielzüge eventuel nur ein placeholder
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


    @MessageMapping("/lobby/ready")
    public void playerReady(PlayerReadyMessage message, Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        // Benachrichtige andere Spieler in der Lobby
        var lobby = lobbyService.getLobby(message.getLobbyId());
        if (lobby != null && lobby.getPlayers().contains(playerId)) {
            messagingTemplate.convertAndSend("/topic/lobby/" + message.getLobbyId(),
                Map.of("type", "PLAYER_READY", "playerId", playerId));
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
}
