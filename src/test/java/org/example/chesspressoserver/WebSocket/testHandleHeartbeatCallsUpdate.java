package org.example.chesspressoserver.WebSocket;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.junit.jupiter.api.Test;
import java.security.Principal;

import static org.mockito.Mockito.*;

class HeartbeatWebSocketControllerTest {

    @Test
    void testHandleHeartbeatCallsUpdate() {
        OnlinePlayerService service = mock(OnlinePlayerService.class);
        HeartbeatWebSocketController controller = new HeartbeatWebSocketController(service);

        Principal principal = () -> "testPlayer";

        HeartbeatWebSocketController.HeartbeatMessage msg = new HeartbeatWebSocketController.HeartbeatMessage();
        msg.setType("heartbeat");

        controller.handleHeartbeat(msg, principal);

        verify(service).updateHeartbeat("testPlayer");
    }
}