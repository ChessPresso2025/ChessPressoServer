package org.example.chesspressoserver.WebSocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    public TestWebSocketController(OnlinePlayerService onlinePlayerService, SimpMessagingTemplate messagingTemplate) {
        this.onlinePlayerService = onlinePlayerService;
        this.messagingTemplate = messagingTemplate;
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestMessage {
        private String content;
        private String timestamp;
    }
}
