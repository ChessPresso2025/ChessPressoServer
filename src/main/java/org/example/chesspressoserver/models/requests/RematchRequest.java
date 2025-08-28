package org.example.chesspressoserver.models.requests;

import lombok.Data;

@Data
public class RematchRequest {
    private String playerId;
    private String lobbyId;
}
