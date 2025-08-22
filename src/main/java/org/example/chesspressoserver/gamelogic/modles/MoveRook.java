package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.List;

public class MoveRook extends PieceMove{
    @Override
    public List<Position> getPossibleMoves(Position start) {
        return List.of();
    }
}
