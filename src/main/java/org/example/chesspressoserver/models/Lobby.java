package org.example.chesspressoserver.models;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.service.LobbyType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Lobby {
    private String lobbyId;
    private org.example.chesspressoserver.service.LobbyType lobbyType;
    private GameTime gameTime;
    private List<String> players;
    private String creator;
    private boolean isGameStarted;
    private LocalDateTime createdAt;
    private LobbyStatus status;

    // Nur für private Lobbys
    private String whitePlayer;
    private String blackPlayer;
    private boolean randomColors;

    public Lobby(String lobbyId, org.example.chesspressoserver.service.LobbyType lobbyType, String creator) {
        this.lobbyId = lobbyId;
        this.lobbyType = lobbyType;
        this.creator = creator;
        this.players = new ArrayList<>();
        this.players.add(creator);
        this.isGameStarted = false;
        this.createdAt = LocalDateTime.now();
        this.status = LobbyStatus.WAITING;
        this.randomColors = false;
    }

    public boolean isFull() {
        return players.size() >= 2;
    }

    public boolean canStart() {
        return players.size() == 2 && gameTime != null;
    }

    public void addPlayer(String playerId) {
        if (!isFull() && !players.contains(playerId)) {
            players.add(playerId);
        }
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
        if (players.isEmpty()) {
            status = LobbyStatus.CLOSED;
        }
    }

    public boolean isPublic() {
        return lobbyType == org.example.chesspressoserver.service.LobbyType.PUBLIC;
    }

    public boolean isPrivate() {
        return lobbyType == org.example.chesspressoserver.service.LobbyType.PRIVATE;
    }
}
