package org.example.chesspressoserver.WebSocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        // Logge alle Handshake-Versuche
        System.out.println("WebSocket handshake attempt from: " + request.getRemoteAddress());
        System.out.println("Request URI: " + request.getURI());
        System.out.println("Headers: " + request.getHeaders());

        // Erlaube alle Handshakes für jetzt (zur Problemdiagnose)
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            System.err.println("WebSocket handshake failed: " + exception.getMessage());
        } else {
            System.out.println("WebSocket handshake successful for: " + request.getRemoteAddress());
        }
    }
}
