package org.example.chesspressoserver.WebSocket;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketUserInspector webSocketUserInspector;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Hauptendpunkt für WebSocket-Verbindungen
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // Zusätzlicher Endpunkt ohne SockJS für native WebSocket-Clients
        registry.addEndpoint("/websocket")
                .setAllowedOriginPatterns("*");
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
        registration.interceptors(webSocketUserInspector);
     }
}
