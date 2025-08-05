package org.example.chesspressoserver.service;

import org.example.chesspressoserver.models.Player;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerService {
    
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    
    /**
     * Findet oder erstellt einen Player basierend auf der Google ID
     * @param googleId Die Google ID des Benutzers
     * @param name Der Name des Benutzers
     * @return Der interne Player
     */
    public Player findOrCreatePlayer(String googleId, String name) {
        return players.computeIfAbsent(googleId, id -> new Player(id, name));
    }
    
    /**
     * Sucht einen Player anhand der Google ID
     * @param googleId Die Google ID
     * @return Der Player oder null falls nicht gefunden
     */
    public Player findPlayerById(String googleId) {
        return players.get(googleId);
    }
    
    /**
     * Aktualisiert den Namen eines Players
     * @param googleId Die Google ID
     * @param name Der neue Name
     */
    public void updatePlayerName(String googleId, String name) {
        Player player = players.get(googleId);
        if (player != null) {
            player.setName(name);
        }
    }
}