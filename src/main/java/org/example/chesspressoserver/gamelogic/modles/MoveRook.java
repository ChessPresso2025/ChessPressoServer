package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveRook extends PieceMove{
    @Override
    public List<Position> getPossibleMoves(Position start) {
        MoveStandard moveStandard = new MoveStandard();
        List<Position> possibleMoves = new ArrayList<>();

        possibleMoves.addAll(moveStandard.horizontalRight(start, board));
        possibleMoves.addAll(moveStandard.horizontalLeft(start, board));
        possibleMoves.addAll(moveStandard.verticalUp(start, board));
        possibleMoves.addAll(moveStandard.verticalDown(start, board));

        return possibleMoves;
    }
}
