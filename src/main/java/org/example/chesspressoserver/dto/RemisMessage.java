package org.example.chesspressoserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.chesspressoserver.models.gamemodels.TeamColor;

@Data
@AllArgsConstructor
public class RemisMessage {
    private String lobbyId;
    private TeamColor requester; // TeamColor
    private TeamColor responder; // TeamColor, null bei Angebot
    private boolean accept;
}
