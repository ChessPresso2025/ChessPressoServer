package org.example.chesspressoserver.WebSocket;
import org.example.chesspressoserver.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

@Component
public class WebSocketUserInspector implements ChannelInterceptor {

    @Autowired
    private JwtService jwtService;

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

                    accessor.setUser(new Principal() {
                        @Override
                        public String getName() {
                            return userId.toString();
                        }
                    });
                    return message;
                } catch (Exception e) {
                    System.err.println("Invalid JWT token in WebSocket connection: " + e.getMessage());
                }
            }

            // Fallback: verwende playerId-Header (für Abwärtskompatibilität)
            String playerId = accessor.getFirstNativeHeader("playerId");
            if (playerId != null) {
                accessor.setUser(new Principal() {
                    @Override
                    public String getName() {
                        return playerId;
                    }
                });
            }
        }

        return message;
    }
}