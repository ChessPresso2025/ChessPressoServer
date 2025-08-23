package org.example.chesspressoserver.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadyMessage {
    private String type;
    private String playerId;
    private String lobbyId;
    private boolean ready;
}
