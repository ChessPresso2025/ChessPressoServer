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

            // Sofortige Aktualisierung der Online-Spieler-Liste
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
            String playerName = user.getName();
            
            // 1. Entferne den Spieler aus allen Lobbys
            try {
                String currentLobbyId = lobbyService.getPlayerLobby(playerName);
                if (currentLobbyId != null) {
                    System.out.println("Player " + playerName + " was in lobby " + currentLobbyId + " - removing from lobby due to disconnect");
                    lobbyService.forceLeaveAllLobbies(playerName);
                    System.out.println("Player " + playerName + " successfully removed from lobby " + currentLobbyId);
                }
            } catch (Exception e) {
                System.out.println("Error removing player " + playerName + " from lobby on disconnect: " + e.getMessage());
            }

            // 2. Entferne den Spieler aus der Online-Liste
            onlinePlayerService.removePlayer(playerName);
            System.out.println("Player " + playerName + " disconnected from WebSocket - removed from online list");
            
            // 3. Verzögerte Aktualisierung um sicherzustellen, dass der Spieler vollständig entfernt wurde
            try {
                // Kurze Verzögerung, um sicherzustellen, dass alle Cleanup-Operationen abgeschlossen sind
                Thread.sleep(100);
                connectionStatusBroadcaster.broadcastPlayerUpdate();
            } catch (Exception e) {
                System.out.println("Error broadcasting player update on disconnect: " + e.getMessage());
            }
        } else {
            // Auch bei anonymen Sessions eine Bereinigung durchführen
            System.out.println("Anonymous session disconnected - performing cleanup");
            try {
                connectionStatusBroadcaster.broadcastPlayerUpdate();
            } catch (Exception e) {
                System.out.println("Error broadcasting player update on anonymous disconnect: " + e.getMessage());
            }
        }
    }
}
