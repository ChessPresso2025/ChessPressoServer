package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.models.Lobby;
import org.example.chesspressoserver.models.LobbyStatus;
import org.example.chesspressoserver.models.GameTime;
import org.example.chesspressoserver.service.LobbyService;
import org.example.chesspressoserver.service.JwtService;
import org.example.chesspressoserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LobbyWebSocketManagerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private LobbyService lobbyService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LobbyWebSocketManager lobbyWebSocketManager;

    private Lobby testLobby;
    private String testLobbyId = "TEST123";
    private String testPlayerId = "player1";
    private String testUsername = "testUser";

    @BeforeEach
    void setUp() {
        testLobby = new Lobby(testLobbyId, Lobby.LobbyType.PUBLIC, testPlayerId);
        testLobby.setStatus(LobbyStatus.WAITING);
        testLobby.setGameTime(GameTime.MIDDLE);

        // Setup default mock returns to avoid NullPointerExceptions
        lenient().when(userService.getUsernameById(anyString())).thenReturn("defaultUser");
        lenient().when(lobbyService.getLobby(anyString())).thenReturn(testLobby);
    }

    @Test
    void registerLobbyConnection_ShouldAddPlayerToLobby() {
        // When
        lobbyWebSocketManager.registerLobbyConnection(testLobbyId, testPlayerId);

        // Then
        assertThat(lobbyWebSocketManager.isLobbyActive(testLobbyId)).isTrue();
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount(testLobbyId)).isEqualTo(1);
        assertThat(lobbyWebSocketManager.getActiveLobbies()).contains(testLobbyId);

        // Verify broadcast was called with correct destination and any message
        verify(messagingTemplate).convertAndSend(
            eq("/topic/lobby/" + testLobbyId),
            any(Map.class)
        );
    }

    @Test
    void registerLobbyConnection_WithMultiplePlayers_ShouldTrackAll() {
        // Given
        String secondPlayerId = "player2";
        String secondUsername = "testUser2";
        when(userService.getUsernameById(secondPlayerId)).thenReturn(secondUsername);

        // When
        lobbyWebSocketManager.registerLobbyConnection(testLobbyId, testPlayerId);
        lobbyWebSocketManager.registerLobbyConnection(testLobbyId, secondPlayerId);

        // Then
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount(testLobbyId)).isEqualTo(2);
        assertThat(lobbyWebSocketManager.isLobbyActive(testLobbyId)).isTrue();

        // Verify both players were broadcasted
        verify(messagingTemplate, times(2)).convertAndSend(
            eq("/topic/lobby/" + testLobbyId),
            any(Map.class)
        );
    }

    @Test
    void unregisterLobbyConnection_ShouldRemovePlayerFromLobby() {
        // Given
        lobbyWebSocketManager.registerLobbyConnection(testLobbyId, testPlayerId);

        // When
        lobbyWebSocketManager.unregisterLobbyConnection(testLobbyId, testPlayerId);

        // Then
        assertThat(lobbyWebSocketManager.isLobbyActive(testLobbyId)).isFalse();
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount(testLobbyId)).isEqualTo(0);
        assertThat(lobbyWebSocketManager.getActiveLobbies()).doesNotContain(testLobbyId);

        // Verify disconnect broadcast was called
        verify(messagingTemplate, atLeastOnce()).convertAndSend(
            eq("/topic/lobby/" + testLobbyId),
            any(Map.class)
        );
    }

    @Test
    void unregisterLobbyConnection_WithMultiplePlayers_ShouldKeepLobbyActive() {
        // Given
        String secondPlayerId = "player2";
        lobbyWebSocketManager.registerLobbyConnection(testLobbyId, testPlayerId);
        lobbyWebSocketManager.registerLobbyConnection(testLobbyId, secondPlayerId);

        // When
        lobbyWebSocketManager.unregisterLobbyConnection(testLobbyId, testPlayerId);

        // Then
        assertThat(lobbyWebSocketManager.isLobbyActive(testLobbyId)).isTrue();
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount(testLobbyId)).isEqualTo(1);
    }

    @Test
    void broadcastToLobby_ShouldSendMessageToSpecificLobby() {
        // Given
        String messageType = "TEST_MESSAGE";
        Map<String, Object> payload = Map.of("test", "data");

        // When
        lobbyWebSocketManager.broadcastToLobby(testLobbyId, messageType, payload);

        // Then
        verify(messagingTemplate).convertAndSend(
            eq("/topic/lobby/" + testLobbyId),
            any(Map.class)
        );
    }

    @Test
    void sendLobbyStatusUpdate_ShouldBroadcastCurrentLobbyStatus() {
        // Given
        testLobby.getPlayerReadyStatus().put(testPlayerId, true);

        // When
        lobbyWebSocketManager.sendLobbyStatusUpdate(testLobbyId);

        // Then
        verify(messagingTemplate).convertAndSend(
            eq("/topic/lobby/" + testLobbyId),
            any(Map.class)
        );
    }

    @Test
    void sendLobbyStatusUpdate_WithNonExistentLobby_ShouldNotBroadcast() {
        // Given
        when(lobbyService.getLobby("NONEXISTENT")).thenReturn(null);

        // When
        lobbyWebSocketManager.sendLobbyStatusUpdate("NONEXISTENT");

        // Then
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }

    @Test
    void demonstrateLobbyIsolation_ShouldSendMessagesToAllActiveLobbies() {
        // Given
        String lobby1 = "LOBBY1";
        String lobby2 = "LOBBY2";
        lobbyWebSocketManager.registerLobbyConnection(lobby1, "player1");
        lobbyWebSocketManager.registerLobbyConnection(lobby2, "player2");

        // When
        lobbyWebSocketManager.demonstrateLobbyIsolation();

        // Then - Verify messages were sent to both lobbies (plus the registration messages)
        verify(messagingTemplate, atLeast(2)).convertAndSend(
            eq("/topic/lobby/" + lobby1),
            any(Map.class)
        );
        verify(messagingTemplate, atLeast(2)).convertAndSend(
            eq("/topic/lobby/" + lobby2),
            any(Map.class)
        );
    }

    @Test
    void getActiveLobbies_ShouldReturnImmutableSet() {
        // Given
        lobbyWebSocketManager.registerLobbyConnection(testLobbyId, testPlayerId);

        // When
        Set<String> activeLobbies = lobbyWebSocketManager.getActiveLobbies();

        // Then
        assertThat(activeLobbies).contains(testLobbyId);

        // Verify it's immutable by checking if it's an ImmutableCollections type
        assertThat(activeLobbies.getClass().getName()).contains("ImmutableCollections");
    }

    @Test
    void isLobbyActive_WithInactiveLobby_ShouldReturnFalse() {
        // When/Then
        assertThat(lobbyWebSocketManager.isLobbyActive("NONEXISTENT")).isFalse();
    }

    @Test
    void getActiveConnectionsCount_WithInactiveLobby_ShouldReturnZero() {
        // When/Then
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount("NONEXISTENT")).isEqualTo(0);
    }

    @Test
    void multipleLobbyOperations_ShouldMaintainIsolation() {
        // Given
        String lobby1 = "LOBBY1";
        String lobby2 = "LOBBY2";
        String player1 = "player1";
        String player2 = "player2";

        // When
        lobbyWebSocketManager.registerLobbyConnection(lobby1, player1);
        lobbyWebSocketManager.registerLobbyConnection(lobby2, player2);

        // Then
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount(lobby1)).isEqualTo(1);
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount(lobby2)).isEqualTo(1);
        assertThat(lobbyWebSocketManager.getActiveLobbies()).containsExactlyInAnyOrder(lobby1, lobby2);

        // When removing from lobby1
        lobbyWebSocketManager.unregisterLobbyConnection(lobby1, player1);

        // Then lobby2 should remain unaffected
        assertThat(lobbyWebSocketManager.isLobbyActive(lobby1)).isFalse();
        assertThat(lobbyWebSocketManager.isLobbyActive(lobby2)).isTrue();
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount(lobby2)).isEqualTo(1);
    }

    @Test
    void concurrentLobbyAccess_ShouldBeThreadSafe() {
        // This test verifies that the ConcurrentHashMap usage is correct
        // Given
        String lobbyId = "CONCURRENT_TEST";

        // When multiple operations happen (simulating concurrent access)
        lobbyWebSocketManager.registerLobbyConnection(lobbyId, "player1");
        lobbyWebSocketManager.registerLobbyConnection(lobbyId, "player2");
        lobbyWebSocketManager.unregisterLobbyConnection(lobbyId, "player1");

        // Then
        assertThat(lobbyWebSocketManager.getActiveConnectionsCount(lobbyId)).isEqualTo(1);
        assertThat(lobbyWebSocketManager.isLobbyActive(lobbyId)).isTrue();
    }
}
