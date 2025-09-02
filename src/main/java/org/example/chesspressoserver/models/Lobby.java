package org.example.chesspressoserver.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Lobby {
    
    // LobbyType enum direkt hier definieren
    public enum LobbyType {
        PRIVATE, PUBLIC
    }
    
    private String lobbyId;
    private LobbyType lobbyType;
    private GameTime gameTime;
    private List<String> players;
    private String creator;
    // Explizite Getter für boolean-Felder
    private boolean gameStarted; // Entferne "is" Prefix für bessere Lombok-Kompatibilität
    private LocalDateTime createdAt;
    private LobbyStatus status;

    // Nur für private Lobbys
    private String whitePlayer;
    private String blackPlayer;
    private boolean randomColors;
    
    // Neue Felder für Ready-Status
    private Map<String, Boolean> playerReadyStatus;

    public Lobby(String lobbyId, LobbyType lobbyType, String creator) {
        this.lobbyId = lobbyId;
        this.lobbyType = lobbyType;
        this.creator = creator;
        this.players = new ArrayList<>();
        this.players.add(creator);
        this.gameStarted = false;
        this.createdAt = LocalDateTime.now();
        this.status = LobbyStatus.WAITING;
        this.randomColors = false;
        this.playerReadyStatus = new HashMap<>();
        this.playerReadyStatus.put(creator, false);
    }

    public boolean isFull() {
        return players.size() >= 2;
    }


    public void addPlayer(String playerId) {
        if (!isFull() && !players.contains(playerId)) {
            players.add(playerId);
            playerReadyStatus.put(playerId, false);
        }
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
        playerReadyStatus.remove(playerId);
    }

    public boolean isPrivate() {
        return lobbyType == LobbyType.PRIVATE;
    }

    public boolean isPublic() {
        return lobbyType == LobbyType.PUBLIC;
    }

    public void setPlayerReady(String playerId, boolean ready) {
        if (players.contains(playerId)) {
            playerReadyStatus.put(playerId, ready);
        }
    }

    public boolean areAllPlayersReady() {
        if (players.size() < 2) {
            return false;
        }
        
        for (String playerId : players) {
            if (!playerReadyStatus.getOrDefault(playerId, false)) {
                return false;
            }
        }
        
        return true;
    }

    public Map<String, Boolean> getPlayerReadyStatus() {
        return new HashMap<>(playerReadyStatus);
    }
}
