package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;

@Component
public class WebSocketEventListener {

    private final OnlinePlayerService onlinePlayerService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(OnlinePlayerService onlinePlayerService, SimpMessagingTemplate messagingTemplate) {
        this.onlinePlayerService = onlinePlayerService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String playerId = user.getName();
            onlinePlayerService.updateHeartbeat(playerId);
            
            // Benachrichtige alle über neuen Online-Spieler
            messagingTemplate.convertAndSend("/topic/player-connected", 
                Map.of("playerId", playerId, "type", "player-connected"));
            
            System.out.println("Spieler verbunden: " + playerId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        
        if (user != null) {
            String playerId = user.getName();
            
            // Benachrichtige alle über Spieler-Trennung
            messagingTemplate.convertAndSend("/topic/player-disconnected", 
                Map.of("playerId", playerId, "type", "player-disconnected"));
            
            System.out.println("Spieler getrennt: " + playerId);
        }
    }
}