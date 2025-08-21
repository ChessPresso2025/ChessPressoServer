package org.example.chesspressoserver.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.chesspressoserver.service.LobbyCodeGenerator;
import org.example.chesspressoserver.models.Lobby.LobbyType;
import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Set;

@Controller
@CrossOrigin(origins = "*")
public class TestWebSocketController {

    private final OnlinePlayerService onlinePlayerService;
    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyCodeGenerator lobbyCodeGenerator;

    public TestWebSocketController(OnlinePlayerService onlinePlayerService,
                                 SimpMessagingTemplate messagingTemplate,
                                 LobbyCodeGenerator lobbyCodeGenerator) {
        this.onlinePlayerService = onlinePlayerService;
        this.messagingTemplate = messagingTemplate;
        this.lobbyCodeGenerator = lobbyCodeGenerator;
    }

    @MessageMapping("/test")
    @SendTo("/topic/test")
    public TestMessage handleTestMessage(TestMessage message, Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";
        return new TestMessage("Server received: " + message.getContent() + " from " + playerId, LocalDateTime.now().toString());
    }

    @MessageMapping("/online-players")
    public void getOnlinePlayers(Principal principal) {
        Set<String> onlinePlayers = onlinePlayerService.getOnlinePlayers();
        String playerId = principal != null ? principal.getName() : "anonymous";

        messagingTemplate.convertAndSendToUser(playerId, "/queue/online-players", onlinePlayers);
    }

    @MessageMapping("/create-lobby")
    public void createLobby(CreateLobbyRequest request, Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";

        String lobbyCode;
        if (request.isPrivate()) {
            lobbyCode = lobbyCodeGenerator.generatePrivateLobbyCode();
        } else {
            lobbyCode = lobbyCodeGenerator.generatePublicLobbyCode();
        }

        LobbyCreatedResponse response = new LobbyCreatedResponse();
        response.setLobbyCode(lobbyCode);
        response.setLobbyType(request.isPrivate() ? "private" : "public");
        response.setTimestamp(LocalDateTime.now().toString());

        messagingTemplate.convertAndSendToUser(playerId, "/queue/lobby-created", response);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestMessage {
        private String content;
        private String timestamp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLobbyRequest {
        private boolean isPrivate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LobbyCreatedResponse {
        private String lobbyCode;
        private String lobbyType;
        private String timestamp;
    }
}
