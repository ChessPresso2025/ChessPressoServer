package org.example.chesspressoserver.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private String type;
    private String playerId;
    private String lobbyId;
    private String message;
    private Long timestamp;
}
