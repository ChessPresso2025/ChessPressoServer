package org.example.chesspressoserver.gamelogic.modles;

import lombok.Getter;
import org.example.chesspressoserver.models.gamemodels.Position;
import org.example.chesspressoserver.models.gamemodels.TeamColor;
import java.util.List;

@Getter
public class MovePawn extends PieceMove {
    private TeamColor color;

    //Construktor
    public MovePawn(TeamColor colour) {
        this.color = colour;
    }

    @Override
    public List<Position> getPossibleMoves(Position start) {
        return List.of();
    }
}
