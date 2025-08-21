package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    private final OnlinePlayerService onlinePlayerService;

    public WebSocketEventListener(OnlinePlayerService onlinePlayerService) {
        this.onlinePlayerService = onlinePlayerService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        System.out.println("WebSocket Connected - Session: " + sessionId + ", User: " + (user != null ? user.getName() : "anonymous"));

        if (user != null) {
            onlinePlayerService.updateHeartbeat(user.getName());
            System.out.println("Player " + user.getName() + " connected via WebSocket");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        System.out.println("WebSocket Disconnected - Session: " + sessionId + ", User: " + (user != null ? user.getName() : "anonymous"));

        if (user != null) {
            onlinePlayerService.removePlayer(user.getName());
            System.out.println("Player " + user.getName() + " disconnected from WebSocket");
        }
    }
}
