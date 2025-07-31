package org.example.chesspressoserver.WebSocket;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
@Component
public class WebSocketUserInspector implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
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