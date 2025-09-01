package org.example.chesspressoserver.models.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StartGameRequest {
    private String lobbyId;
    private String gameTime;
    private String whitePlayer;
    private String blackPlayer;
    private boolean randomPlayers;
    //lobby channel?
}

