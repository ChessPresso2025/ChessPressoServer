package org.example.chesspressoserver.models.requests;

import lombok.Data;

@Data
public class ResignGameRequest {
    private String playerId;
    private String lobbyId;
}
