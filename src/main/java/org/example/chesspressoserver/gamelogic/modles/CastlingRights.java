package org.example.chesspressoserver.gamelogic.modles;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.models.gamemodels.TeamColor;

@Getter
@Setter
public class CastlingRights {
    private boolean whiteKingSide = true;
    private boolean whiteQueenSide = true;
    private boolean blackKingSide = true;
    private boolean blackQueenSide = true;
}
