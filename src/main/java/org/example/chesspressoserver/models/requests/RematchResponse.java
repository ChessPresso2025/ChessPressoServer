package org.example.chesspressoserver.models.requests;

public class RematchResponse {
    private String type = "rematch-response";
    private String lobbyId;
    private String playerId;
    private String response; // "accepted" oder "declined"

    public RematchResponse() {}

    public RematchResponse(String lobbyId, String playerId, String response) {
        this.lobbyId = lobbyId;
        this.playerId = playerId;
        this.response = response;
    }

    public String getType() { return type; }
    public String getLobbyId() { return lobbyId; }
    public void setLobbyId(String lobbyId) { this.lobbyId = lobbyId; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}

