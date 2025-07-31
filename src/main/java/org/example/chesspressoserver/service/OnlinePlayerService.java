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
    private final Duration TIMEOUT = Duration.ofSeconds(5);

    public void updateHeartbeat(String playerId) {
        lastSeenMap.put(playerId, Instant.now());
    }

    public Set<String> getOnlinePlayers() {
        Instant now = Instant.now();
        return lastSeenMap.entrySet().stream()
                .filter(entry -> Duration.between(entry.getValue(), now).compareTo(TIMEOUT) < 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public void cleanup() {
        Instant now = Instant.now();
        lastSeenMap.entrySet().removeIf(entry -> Duration.between(entry.getValue(), now).compareTo(TIMEOUT) < 0);
    }
}
