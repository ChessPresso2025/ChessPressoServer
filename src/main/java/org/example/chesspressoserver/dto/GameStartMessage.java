package org.example.chesspressoserver.dto;

import lombok.*;
import org.example.chesspressoserver.models.GameTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameStartMessage {
    private String type;
    private String lobbyId;
    private GameTime gameTime;
    private String whitePlayer;
    private String blackPlayer;
    private String lobbyChannel;
}
