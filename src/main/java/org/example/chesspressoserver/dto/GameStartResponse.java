package org.example.chesspressoserver.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameStartResponse {
    private boolean success;
    private String lobbyId;
    private String gameTime;
    private String whitePlayer;
    private String blackPlayer;
    private String lobbyChannel;
    private Map<String, Object> board; // PieceInfo später ersetzen
    private String error;
}

