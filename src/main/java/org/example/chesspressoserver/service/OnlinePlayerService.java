package org.example.chesspressoserver.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OnlinePlayerService {

    private final Map<String, Instant> lastSeenMap = new ConcurrentHashMap<>();
    private final Duration TIMEOUT = Duration.ofSeconds(45); // Reduziert auf 45 Sekunden - Client sendet alle 30 Sekunden

    public void updateHeartbeat(String playerId) {
        if (playerId != null && !playerId.equals("anonymous")) {
            lastSeenMap.put(playerId, Instant.now());
        }
    }

    public Set<String> getOnlinePlayers() {
        Instant now = Instant.now();
        return lastSeenMap.entrySet().stream()
                .filter(entry -> Duration.between(entry.getValue(), now).compareTo(TIMEOUT) < 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public boolean isPlayerOnline(String playerId) {
        if (playerId == null || playerId.equals("anonymous")) {
            return false;
        }
        Instant lastSeen = lastSeenMap.get(playerId);
        if (lastSeen == null) {
            return false;
        }
        return Duration.between(lastSeen, Instant.now()).compareTo(TIMEOUT) < 0;
    }

    public void removePlayer(String playerId) {
        lastSeenMap.remove(playerId);
    }

    public void cleanup() {
        Instant now = Instant.now();
        Set<String> playersToRemove = lastSeenMap.entrySet().stream()
                .filter(entry -> Duration.between(entry.getValue(), now).compareTo(TIMEOUT) >= 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        
        playersToRemove.forEach(lastSeenMap::remove);
        
        if (!playersToRemove.isEmpty()) {
            System.out.println("Removed inactive players: " + playersToRemove);
        }
    }

    public int getOnlinePlayerCount() {
        return getOnlinePlayers().size();
    }
}