package org.example.chesspressoserver.models.requests;

import lombok.Data;

@Data
public class StartGameRequest {
    private String lobbyId;
    private String playerId;
    // Optional: weitere Parameter wie Spielzeit, Farbe, etc.
}

