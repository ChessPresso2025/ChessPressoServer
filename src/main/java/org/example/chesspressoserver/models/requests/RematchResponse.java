package org.example.chesspressoserver.models.requests;
import lombok.Getter;
import lombok.Setter;

public class RematchResponse {
    private String type = "rematch-response";
    @Setter
    private String lobbyId;
    @Setter
    private String playerId;
    @Setter
    private String response; // "accepted" oder "declined"



    public String getType() { return type; }
    public String getLobbyId() { return lobbyId; }

    public String getPlayerId() { return playerId; }

    public String getResponse() { return response; }
}

