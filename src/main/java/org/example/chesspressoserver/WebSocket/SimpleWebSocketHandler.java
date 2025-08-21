package org.example.chesspressoserver.WebSocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.example.chesspressoserver.service.OnlinePlayerService;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionToPlayerId = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private OnlinePlayerService onlinePlayerService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket verbunden: " + session.getId());
        sessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            System.out.println("Nachricht erhalten: " + payload);

            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.get("type").asText();

            if ("heartbeat".equals(type)) {
                String playerId = jsonNode.get("playerId").asText();
                String lobbyId = jsonNode.get("lobbyId").asText();

                // PlayerId mit Session verknüpfen
                sessionToPlayerId.put(session.getId(), playerId);

                // Player als online markieren
                onlinePlayerService.updateHeartbeat(playerId);

                // Heartbeat bestätigen
                String response = "{\"type\":\"heartbeat_ack\",\"status\":\"ok\"}";
                session.sendMessage(new TextMessage(response));
            }

        } catch (Exception e) {
            System.err.println("Fehler beim Verarbeiten der Nachricht: " + e.getMessage());
            String errorResponse = "{\"type\":\"error\",\"message\":\"Invalid message format\"}";
            session.sendMessage(new TextMessage(errorResponse));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket getrennt: " + session.getId());
        sessions.remove(session.getId());
        sessionToPlayerId.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket Transport Error: " + exception.getMessage());
        sessions.remove(session.getId());
        sessionToPlayerId.remove(session.getId());
    }

    // Server-zu-Client Broadcast alle 5 Sekunden
    // @Scheduled(fixedRate = 5000) // Deaktiviert - wird nicht verwendet, da STOMP WebSocket benutzt wird
    public void broadcastServerStatus() {
        String statusMessage = "{\"type\":\"server_status\",\"status\":\"online\",\"timestamp\":" + System.currentTimeMillis() + "}";

        // An alle verbundenen Clients senden
        sessions.forEach((sessionId, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(statusMessage));
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Senden der Server-Status-Nachricht an Session " + sessionId + ": " + e.getMessage());
                // Defekte Session entfernen
                sessions.remove(sessionId);
                sessionToPlayerId.remove(sessionId);
            }
        });

        // System.out.println("Server-Status an " + sessions.size() + " Clients gesendet"); // Deaktiviert
    }
}
