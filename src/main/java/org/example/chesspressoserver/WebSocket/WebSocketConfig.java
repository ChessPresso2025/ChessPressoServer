package org.example.chesspressoserver.WebSocket;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Vereinfachter Hauptendpunkt ohne SockJS für bessere Kompatibilität
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new WebSocketHandshakeInterceptor());

        // SockJS-Endpoint mit korrekter Konfiguration
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setInterceptors(new WebSocketHandshakeInterceptor());

        // Zusätzlicher nativer WebSocket-Endpoint
        registry.addEndpoint("/websocket")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new WebSocketHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Broker für Topics und Queues
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix für Client-zu-Server Nachrichten
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix für benutzerspezifische Nachrichten
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
