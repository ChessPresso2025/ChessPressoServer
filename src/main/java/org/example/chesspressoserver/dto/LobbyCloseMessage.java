package org.example.chesspressoserver.dto;

public class LobbyCloseMessage {
    private String lobbyId;
    private String playerId;
    private String type = "lobby-close";

    public LobbyCloseMessage() {}

    public LobbyCloseMessage(String lobbyId, String playerId) {
        this.lobbyId = lobbyId;
        this.playerId = playerId;
    }

    public String getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(String lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

