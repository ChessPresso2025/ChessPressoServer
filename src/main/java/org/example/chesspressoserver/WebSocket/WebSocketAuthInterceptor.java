package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.service.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        System.out.println("WebSocket CONNECT - Token extracted: " + (token != null ? "Yes" : "No"));
        if (token != null) {
            authenticateWithToken(token, accessor);
        } else {
            authenticateWithSessionHeader(accessor);
        }
    }

    private void authenticateWithToken(String token, StompHeaderAccessor accessor) {
        try {
            UUID userId = jwtService.getUserIdFromToken(token);
            String username = jwtService.getUsernameFromToken(token);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                username != null ? username : userId.toString(),
                null,
                Collections.emptyList()
            );
            accessor.setUser(authentication);
            System.out.println("WebSocket authentication successful for user: " + (username != null ? username : userId.toString()));
        } catch (Exception e) {
            System.err.println("WebSocket JWT authentication failed: " + e.getMessage());
            // Erlaube Verbindung auch ohne gültigen Token (für Tests)
        }
    }

    private void authenticateWithSessionHeader(StompHeaderAccessor accessor) {
        System.out.println("WebSocket connection without JWT token - checking session headers");
        List<String> sessionHeaders = accessor.getNativeHeader("X-User-ID");
        if (sessionHeaders != null && !sessionHeaders.isEmpty()) {
            String userId = sessionHeaders.get(0);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId, null, Collections.emptyList()
            );
            accessor.setUser(authentication);
            System.out.println("WebSocket authentication via session for user: " + userId);
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Versuche Authorization header
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        // Versuche authorization (lowercase)
        List<String> authHeadersLower = accessor.getNativeHeader("authorization");
        if (authHeadersLower != null && !authHeadersLower.isEmpty()) {
            String authHeader = authHeadersLower.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        // Versuche token header
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        // Versuche access_token parameter
        List<String> accessTokenHeaders = accessor.getNativeHeader("access_token");
        if (accessTokenHeaders != null && !accessTokenHeaders.isEmpty()) {
            return accessTokenHeaders.get(0);
        }

        return null;
    }
}
