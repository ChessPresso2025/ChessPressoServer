package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.JwtService;
import org.example.chesspressoserver.service.UserService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service zur Überwachung und Verwaltung von Lobby-WebSocket-Verbindungen
 * Stellt sicher, dass jede Lobby isoliert läuft
 */
@Component
public class LobbyWebSocketManager {

    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyService lobbyService;
    private final JwtService jwtService;
    private final UserService userService;

    // Verfolgt aktive WebSocket-Verbindungen pro Lobby
    private final Map<String, Set<String>> lobbyConnections = new ConcurrentHashMap<>();

    // Verfolgt welche Lobbies aktiv sind
    private final Set<String> activeLobbies = ConcurrentHashMap.newKeySet();

    private static final String PLAYER_ID = "playerId";
    private static final String USERNAME = "username";

    public LobbyWebSocketManager(SimpMessagingTemplate messagingTemplate,
                                LobbyService lobbyService,
                                JwtService jwtService,
                                UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.lobbyService = lobbyService;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * Registriert eine neue WebSocket-Verbindung für eine Lobby
     */
    public void registerLobbyConnection(String lobbyId, String playerId) {
        lobbyConnections.computeIfAbsent(lobbyId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
        activeLobbies.add(lobbyId);
        String username = userService.getUsernameById(playerId);
        // Fallback falls username null ist
        if (username == null) {
            username = "Unknown User";
        }
        // Informiere alle Spieler in der Lobby über die neue Verbindung
        broadcastToLobby(lobbyId, "PLAYER_CONNECTED", Map.of(
            PLAYER_ID, playerId,
            USERNAME, username,
            "connectedPlayers", lobbyConnections.get(lobbyId).size()
        ));
    }

    /**
     * Entfernt eine WebSocket-Verbindung aus einer Lobby
     */
    public void unregisterLobbyConnection(String lobbyId, String playerId) {
        Set<String> connections = lobbyConnections.get(lobbyId);
        if (connections != null) {
            connections.remove(playerId);

            if (connections.isEmpty()) {
                lobbyConnections.remove(lobbyId);
                activeLobbies.remove(lobbyId);
            } else {
                // Informiere verbleibende Spieler über Disconnection
                broadcastToLobby(lobbyId, "PLAYER_DISCONNECTED", Map.of(
                    PLAYER_ID, playerId,
                    USERNAME, userService.getUsernameById(playerId),
                    "connectedPlayers", connections.size()
                ));
            }
        }
    }

    /**
     * Sendet eine Nachricht an alle Spieler in einer spezifischen Lobby
     * Dies demonstriert die Lobby-Isolation
     */
    public void broadcastToLobby(String lobbyId, String messageType, Object payload) {
        // Nur an die spezifische Lobby senden - keine anderen Lobbies erhalten diese Nachricht
        messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, Map.of(
            "type", messageType,
            "lobbyId", lobbyId,
            "payload", payload != null ? payload : Map.of(),
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Sendet Lobby-Status-Update nur an die betroffene Lobby
     */
    public void sendLobbyStatusUpdate(String lobbyId) {
        var lobby = lobbyService.getLobby(lobbyId);
        if (lobby == null) return;

        // Erstelle Spielerliste mit dekodierten Namen
        var playersInfo = lobby.getPlayers().stream()
            .map(playerId -> Map.of(
                PLAYER_ID, playerId,
                USERNAME, userService.getUsernameById(playerId),
                "ready", lobby.getPlayerReadyStatus().getOrDefault(playerId, false)
            ))
            .toList();

        // Sende Update nur an diese spezifische Lobby
        broadcastToLobby(lobbyId, "LOBBY_STATUS_UPDATE", Map.of(
            "players", playersInfo,
            "status", lobby.getStatus().toString(),
            "gameStarted", lobby.isGameStarted(),
            "gameTime", lobby.getGameTime()
        ));
    }

    /**
     * Prüft ob eine Lobby aktive WebSocket-Verbindungen hat
     */
    public boolean isLobbyActive(String lobbyId) {
        return activeLobbies.contains(lobbyId);
    }

    /**
     * Gibt die Anzahl aktiver Verbindungen für eine Lobby zurück
     */
    public int getActiveConnectionsCount(String lobbyId) {
        Set<String> connections = lobbyConnections.get(lobbyId);
        return connections != null ? connections.size() : 0;
    }

    /**
     * Gibt alle aktiven Lobbies zurück
     */
    public Set<String> getActiveLobbies() {
        return Set.copyOf(activeLobbies);
    }

    /**
     * Demonstriert die Lobby-Isolation: Sendet verschiedene Nachrichten an verschiedene Lobbies
     */
    public void demonstrateLobbyIsolation() {
        // Beispiel: Sende unterschiedliche Nachrichten an verschiedene Lobbies
        for (String lobbyId : activeLobbies) {
            broadcastToLobby(lobbyId, "ISOLATION_TEST", Map.of(
                "message", "Dies ist eine isolierte Nachricht nur für Lobby " + lobbyId,
                "lobbyId", lobbyId,
                "otherActiveLobbies", activeLobbies.size() - 1
            ));
        }
    }
}
