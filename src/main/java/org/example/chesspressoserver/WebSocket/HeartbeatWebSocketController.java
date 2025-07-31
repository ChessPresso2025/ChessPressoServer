package org.example.chesspressoserver.WebSocket;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class HeartbeatWebSocketController {

    private final OnlinePlayerService onlinePlayerService;

    public HeartbeatWebSocketController(OnlinePlayerService onlinePlayerService) {
        this.onlinePlayerService = onlinePlayerService;
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(HeartbeatMessage message, Principal principal) {
        String playerId = principal.getName();
        onlinePlayerService.updateHeartbeat(playerId);
    }

    @Setter
    @Getter
    public static class HeartbeatMessage {
        private String type;
    }


}
