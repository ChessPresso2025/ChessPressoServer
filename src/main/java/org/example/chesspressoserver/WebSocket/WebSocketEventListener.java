package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.components.ConnectionStatusBroadcaster;
import org.example.chesspressoserver.service.LobbyService;
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
    private final ConnectionStatusBroadcaster connectionStatusBroadcaster;
    private final LobbyService lobbyService;

    public WebSocketEventListener(OnlinePlayerService onlinePlayerService, ConnectionStatusBroadcaster connectionStatusBroadcaster, LobbyService lobbyService) {
        this.onlinePlayerService = onlinePlayerService;
        this.connectionStatusBroadcaster = connectionStatusBroadcaster;
        this.lobbyService = lobbyService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        System.out.println("WebSocket Connected - Session: " + sessionId + ", User: " + (user != null ? user.getName() : "anonymous"));

        if (user != null && !user.getName().equals("anonymous")) {
            String userName = user.getName();
            onlinePlayerService.updateHeartbeat(userName);
            System.out.println("Player " + userName + " connected via WebSocket");

            try {
                connectionStatusBroadcaster.broadcastPlayerUpdate();
            } catch (Exception e) {
                System.out.println("Error broadcasting player update on connect: " + e.getMessage());
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();

        System.out.println("WebSocket Disconnected - Session: " + sessionId + ", User: " + (user != null ? user.getName() : "anonymous"));

        if (user != null && !user.getName().equals("anonymous")) {
            String userName = user.getName();
            
            // Entferne den Spieler aus seiner aktiven Lobby
            lobbyService.removePlayerFromActiveLobby(userName);
            System.out.println("Player " + userName + " removed from active lobby");

            try {
                connectionStatusBroadcaster.broadcastPlayerUpdate();
            } catch (Exception e) {
                System.out.println("Error broadcasting player update on disconnect: " + e.getMessage());
            }
        }
    }
}
