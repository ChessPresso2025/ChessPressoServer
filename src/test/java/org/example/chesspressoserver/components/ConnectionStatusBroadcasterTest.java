package org.example.chesspressoserver.components;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ConnectionStatusBroadcasterTest {

    private OnlinePlayerService onlinePlayerService;
    private SimpMessagingTemplate messagingTemplate;
    private ConnectionStatusBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        onlinePlayerService = mock(OnlinePlayerService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        broadcaster = new ConnectionStatusBroadcaster(messagingTemplate, onlinePlayerService);
    }

    @Test
    void broadcastPlayerUpdate_shouldSendStatusToEachPlayer() {
        // Vorbereitung
        Set<String> onlinePlayers = Set.of("player1", "player2");
        when(onlinePlayerService.getOnlinePlayers()).thenReturn(onlinePlayers);

        // Ausführung
        broadcaster.broadcastPlayerUpdate();

        // Stelle sicher, dass die öffentliche Nachricht gesendet wurde
        verify(messagingTemplate).convertAndSend(eq("/topic/players"), anyMap());
    }
    @Test
    void broadcastConnectionStatus_shouldCallBroadcastPlayerUpdate() {
        // Vorbereitung
        ConnectionStatusBroadcaster spyBroadcaster = spy(broadcaster);

        // Ausführung
        spyBroadcaster.broadcastConnectionStatus();

        // Überprüfung
        verify(spyBroadcaster).broadcastPlayerUpdate();
    }
}
