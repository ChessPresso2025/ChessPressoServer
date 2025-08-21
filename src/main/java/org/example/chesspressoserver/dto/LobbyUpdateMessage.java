package org.example.chesspressoserver.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LobbyUpdateMessage {
    private String type;
    private String lobbyId;
    private String playerId;
    private List<String> players;
    private String status;
    private String message;
}
