package org.example.chesspressoserver.WebSocket;
import org.example.chesspressoserver.service.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.UUID;

@Component
public class WebSocketUserInspector implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketUserInspector.class);

    private final JwtService jwtService;

    public WebSocketUserInspector(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Versuche zuerst JWT-Token aus Authorization-Header zu lesen
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    UUID userId = jwtService.getUserIdFromToken(token);
                    accessor.setUser(() -> userId.toString());
                    return message;
                } catch (Exception e) {
                    logger.warn("Invalid JWT token in WebSocket connection: {}", e.getMessage());
                }
            }

            // Fallback: verwende playerId-Header (für Abwärtskompatibilität)
            String playerId = accessor.getFirstNativeHeader("playerId");
            if (playerId != null) {
                accessor.setUser(() -> playerId);
            }
        }

        return message;
    }
}