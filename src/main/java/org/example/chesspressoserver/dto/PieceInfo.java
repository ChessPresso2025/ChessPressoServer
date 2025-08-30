package org.example.chesspressoserver.dto;

import lombok.Data;
import org.example.chesspressoserver.models.gamemodels.PieceType;
import org.example.chesspressoserver.models.gamemodels.TeamColor;

@Data
public class PieceInfo {
    private PieceType type;
    private TeamColor colour;

    public PieceInfo(PieceType type, TeamColor colour) {
        this.type = type;
        this.colour = colour;
    }
}

