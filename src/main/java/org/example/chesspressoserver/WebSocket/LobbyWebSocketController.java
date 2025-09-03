package org.example.chesspressoserver.WebSocket;


import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.JwtService;
import org.example.chesspressoserver.service.UserService;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

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
     * Sendet eine Fehlernachricht an den User
     */
    private void sendErrorToUser(String playerId, String message) {
        messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-error", Map.of("error", message));
    }

}
