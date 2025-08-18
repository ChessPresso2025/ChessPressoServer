package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.models.Player;
import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Set;

@Controller
public class GameWebSocketController {

    private final OnlinePlayerService onlinePlayerService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(OnlinePlayerService onlinePlayerService, SimpMessagingTemplate messagingTemplate) {
        this.onlinePlayerService = onlinePlayerService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/connect")
    @SendTo("/topic/players")
    public String handlePlayerConnect(Player player, Principal principal) {
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
    public String handleGameJoin(GameMessage message, Principal principal) {
        String playerId = principal != null ? principal.getName() : "anonymous";
        onlinePlayerService.updateHeartbeat(playerId);
        return "Player " + playerId + " wants to join game";
    }

    public static class GameMessage {
        private String type;
        private String content;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}