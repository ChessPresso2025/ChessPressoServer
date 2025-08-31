package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.components.ConnectionStatusBroadcaster;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.OnlinePlayerService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketEventListener {
    private final OnlinePlayerService onlinePlayerService;
    private final ConnectionStatusBroadcaster connectionStatusBroadcaster;
    private final LobbyService lobbyService;
    private final LobbyWebSocketManager lobbyManager;

    // Pattern um Lobby-IDs aus Subscription-Destinations zu extrahieren
    private static final Pattern LOBBY_TOPIC_PATTERN = Pattern.compile("/topic/lobby/([a-zA-Z0-9]+)");

    public WebSocketEventListener(OnlinePlayerService onlinePlayerService,
                                ConnectionStatusBroadcaster connectionStatusBroadcaster,
                                LobbyService lobbyService,
                                LobbyWebSocketManager lobbyManager) {
        this.onlinePlayerService = onlinePlayerService;
        this.connectionStatusBroadcaster = connectionStatusBroadcaster;
        this.lobbyService = lobbyService;
        this.lobbyManager = lobbyManager;
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

    /**
     * Behandelt WebSocket-Subscriptions zu Lobby-Topics
     * Registriert automatisch Spieler in der entsprechenden Lobby
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();
        String destination = headerAccessor.getDestination();

        if (user != null && destination != null) {
            // Prüfe ob es sich um eine Lobby-Subscription handelt
            Matcher matcher = LOBBY_TOPIC_PATTERN.matcher(destination);
            if (matcher.matches()) {
                String lobbyId = matcher.group(1);
                String playerId = user.getName();

                System.out.println("Player " + playerId + " subscribed to lobby " + lobbyId);

                // Registriere die Verbindung in der Lobby-Verwaltung
                lobbyManager.registerLobbyConnection(lobbyId, playerId);

                // Sende aktuellen Lobby-Status an den neuen Subscriber
                try {
                    lobbyManager.sendLobbyStatusUpdate(lobbyId);
                } catch (Exception e) {
                    System.out.println("Error sending lobby status update: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Behandelt WebSocket-Unsubscriptions von Lobby-Topics
     * Entfernt automatisch Spieler aus der entsprechenden Lobby
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        // Bei Unsubscribe müssen wir die ursprüngliche Destination aus dem Session-Attribut abrufen
        String destination = (String) headerAccessor.getSessionAttributes().get("lastSubscription");

        if (user != null && destination != null) {
            Matcher matcher = LOBBY_TOPIC_PATTERN.matcher(destination);
            if (matcher.matches()) {
                String lobbyId = matcher.group(1);
                String playerId = user.getName();

                System.out.println("Player " + playerId + " unsubscribed from lobby " + lobbyId);

                // Entferne die Verbindung aus der Lobby-Verwaltung
                lobbyManager.unregisterLobbyConnection(lobbyId, playerId);
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
            System.out.println("Player " + userName + " disconnected from WebSocket");

            // Entferne Spieler aus ALLEN Lobbies bei Disconnect
            for (String lobbyId : lobbyManager.getActiveLobbies()) {
                lobbyManager.unregisterLobbyConnection(lobbyId, userName);
            }

            // Entferne aus OnlinePlayerService nach kurzer Verzögerung
            // (um Reconnection-Versuche zu ermöglichen)
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // 5 Sekunden warten
                    onlinePlayerService.removePlayer(userName);
                    connectionStatusBroadcaster.broadcastPlayerUpdate();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.out.println("Error removing player on disconnect: " + e.getMessage());
                }
            }).start();
        }
    }
}
