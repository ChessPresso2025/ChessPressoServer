package org.example.chesspressoserver.models.requests;

public class RematchResult {
    private String type = "rematch-result";
    private String lobbyId;
    private String result; // "accepted" oder "declined"

    public RematchResult() {}

    public RematchResult(String lobbyId, String result) {
        this.lobbyId = lobbyId;
        this.result = result;
    }

    public String getType() { return type; }
    public String getLobbyId() { return lobbyId; }
    public void setLobbyId(String lobbyId) { this.lobbyId = lobbyId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}

