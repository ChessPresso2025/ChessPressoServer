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
    void testBroadcastsToAllOnlinePlayers() {
        when(onlinePlayerService.getOnlinePlayers()).thenReturn(Set.of("abc", "def"));

        broadcaster.broadcastConnectionStatus();

        verify(messagingTemplate, times(2)).convertAndSendToUser(
                anyString(), eq("/queue/status"), anyMap()
        );

        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> messageCaptor = ArgumentCaptor.forClass(Map.class);

        verify(messagingTemplate, times(2)).convertAndSendToUser(
                playerIdCaptor.capture(), eq("/queue/status"), messageCaptor.capture()
        );

        assertEquals(Set.of("abc", "def"), Set.copyOf(playerIdCaptor.getAllValues()));
        for (Map<String, String> msg : messageCaptor.getAllValues()) {
            assertEquals("connection-status", msg.get("type"));
            assertEquals("online", msg.get("status"));
        }
    }
}
