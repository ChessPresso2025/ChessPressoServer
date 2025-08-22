package org.example.chesspressoserver.serice;

import org.example.chesspressoserver.service.OnlinePlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class OnlinePlayerServiceTest {

    private OnlinePlayerService service;

    @BeforeEach
    void setUp() {
        service = new OnlinePlayerService();
    }

    @Test
    void testUpdateHeartbeatStoresTimestamp() {
        service.updateHeartbeat("player1");
        Set<String> online = service.getOnlinePlayers();
        assertTrue(online.contains("player1"));
    }

    @Test
    void testCleanupRemovesExpiredPlayers() throws Exception {
        // Füge einen Spieler hinzu
        service.updateHeartbeat("expiredPlayer");

        // Verwende Reflection um das lastSeenMap direkt zu manipulieren
        Field lastSeenMapField = OnlinePlayerService.class.getDeclaredField("lastSeenMap");
        lastSeenMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Instant> lastSeenMap = (Map<String, Instant>) lastSeenMapField.get(service);

        // Setze den Zeitstempel auf eine Zeit vor 46 Sekunden (älter als das 45s Timeout)
        lastSeenMap.put("expiredPlayer", Instant.now().minus(Duration.ofSeconds(46)));

        // Führe cleanup aus
        service.cleanup();

        // Überprüfe, dass der Spieler entfernt wurde
        Set<String> online = service.getOnlinePlayers();
        assertFalse(online.contains("expiredPlayer"));
    }

    @Test
    void testIsPlayerOnlineReturnsTrueForActivePlayer() {
        service.updateHeartbeat("activePlayer");
        assertTrue(service.isPlayerOnline("activePlayer"));
    }

    @Test
    void testIsPlayerOnlineReturnsFalseForAnonymous() {
        assertFalse(service.isPlayerOnline("anonymous"));
        assertFalse(service.isPlayerOnline(null));
    }

    @Test
    void testRemovePlayer() {
        service.updateHeartbeat("playerToRemove");
        assertTrue(service.isPlayerOnline("playerToRemove"));

        service.removePlayer("playerToRemove");
        assertFalse(service.isPlayerOnline("playerToRemove"));
    }
}