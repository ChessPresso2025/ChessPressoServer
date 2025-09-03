package org.example.chesspressoserver.WebSocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        // Logge alle Handshake-Versuche
        logger.info("WebSocket handshake attempt from: {}", request.getRemoteAddress());
        logger.info("Request URI: {}", request.getURI());
        logger.info("Headers: {}", request.getHeaders());

        // Erlaube alle Handshakes für jetzt (zur Problemdiagnose)
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            logger.error("WebSocket handshake failed: {}", exception.getMessage());
        } else {
            logger.info("WebSocket handshake successful for: {}", request.getRemoteAddress());
        }
    }
}
