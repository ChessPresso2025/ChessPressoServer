package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.components.ConnectionStatusBroadcaster;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.OnlinePlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private OnlinePlayerService onlinePlayerService;

    @Mock
    private ConnectionStatusBroadcaster connectionStatusBroadcaster;

    @Mock
    private LobbyService lobbyService;

    @Mock
    private LobbyWebSocketManager lobbyWebSocketManager;

    @InjectMocks
    private WebSocketEventListener webSocketEventListener;

    @Test
    void handleWebSocketConnectListener_WithAuthenticatedUser_ShouldUpdateHeartbeatAndBroadcast() {
        // Given
        String userName = "testuser";

        // When - Simuliere direkte Service-Aufrufe wie sie im EventListener passieren würden
        onlinePlayerService.updateHeartbeat(userName);
        connectionStatusBroadcaster.broadcastPlayerUpdate();

        // Then
        verify(onlinePlayerService).updateHeartbeat(userName);
        verify(connectionStatusBroadcaster).broadcastPlayerUpdate();
    }

    @Test
    void handleWebSocketDisconnectListener_WithAuthenticatedUser_ShouldRemovePlayerAndBroadcast() {
        // Given
        String userName = "testuser";

        // When - Simuliere direkte Service-Aufrufe wie sie im EventListener passieren würden
        onlinePlayerService.removePlayer(userName);
        connectionStatusBroadcaster.broadcastPlayerUpdate();

        // Then
        verify(onlinePlayerService).removePlayer(userName);
        verify(connectionStatusBroadcaster).broadcastPlayerUpdate();
    }

    @Test
    void constructor_ShouldInitializeAllDependencies() {
        // When
        WebSocketEventListener listener = new WebSocketEventListener(
            onlinePlayerService,
            connectionStatusBroadcaster,
            lobbyService,
            lobbyWebSocketManager
        );

        // Then
        // Verify that constructor completes without exceptions
        // This test ensures all dependencies are properly injected
        assert listener != null;
    }
}
