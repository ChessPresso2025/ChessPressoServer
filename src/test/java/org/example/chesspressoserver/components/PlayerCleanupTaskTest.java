package org.example.chesspressoserver.components;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlayerCleanupTaskTest {

    @Mock
    private OnlinePlayerService onlinePlayerService;

    @InjectMocks
    private PlayerCleanupTask playerCleanupTask;

    @Test
    void cleanupInactivePlayers_ShouldCallOnlinePlayerServiceCleanup() {
        // When
        playerCleanupTask.cleanupInactivePlayers();

        // Then
        verify(onlinePlayerService).cleanup();
    }
}
