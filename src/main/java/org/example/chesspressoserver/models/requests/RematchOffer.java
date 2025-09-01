package org.example.chesspressoserver.models.requests;

public class RematchOffer {
    private String type = "rematch-offer";
    private String lobbyId;
    private String fromPlayerId;
    private String toPlayerId;

    public RematchOffer() {}

    public RematchOffer(String lobbyId, String fromPlayerId, String toPlayerId) {
        this.lobbyId = lobbyId;
        this.fromPlayerId = fromPlayerId;
        this.toPlayerId = toPlayerId;
    }

    public String getType() { return type; }
    public String getLobbyId() { return lobbyId; }
    public void setLobbyId(String lobbyId) { this.lobbyId = lobbyId; }
    public String getFromPlayerId() { return fromPlayerId; }
    public void setFromPlayerId(String fromPlayerId) { this.fromPlayerId = fromPlayerId; }
    public String getToPlayerId() { return toPlayerId; }
    public void setToPlayerId(String toPlayerId) { this.toPlayerId = toPlayerId; }
}
