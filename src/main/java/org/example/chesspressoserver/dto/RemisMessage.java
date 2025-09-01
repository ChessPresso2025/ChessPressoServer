package org.example.chesspressoserver.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.chesspressoserver.models.gamemodels.TeamColor;

@Data
@AllArgsConstructor
public class RemisMessage {
    private String lobbyId;
    private TeamColor requester;
    @Nullable
    private TeamColor responder; // null bei Anfrage, gesetzt bei Annahme
    private boolean accept;
}
