package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.List;

public class MoveKing implements PieceMove{
    @Override
    public List<Position> getPossibleMoves(Position start) {
        return List.of();
    }
}
