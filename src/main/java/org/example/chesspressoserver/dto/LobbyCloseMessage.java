package org.example.chesspressoserver.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LobbyCloseMessage {
    private String lobbyId;
    private String playerId;
    private String type = "lobby-close";

    public LobbyCloseMessage() {}

    public LobbyCloseMessage(String lobbyId, String playerId) {
        this.lobbyId = lobbyId;
        this.playerId = playerId;
    }

}

