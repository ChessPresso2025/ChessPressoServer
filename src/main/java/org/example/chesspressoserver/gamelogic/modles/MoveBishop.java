package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveBishop extends PieceMove{
    @Override
    public List<Position> getPossibleMoves(Position start) {
        MoveStandard moveStandard = new MoveStandard();
        List<Position> possibleMoves = new ArrayList<>();

        possibleMoves.addAll(moveStandard.diagonalUpRight(start, board));
        possibleMoves.addAll(moveStandard.diagonalUpLeft(start, board));
        possibleMoves.addAll(moveStandard.diagonalDownRight(start, board));
        possibleMoves.addAll(moveStandard.diagonalDownLeft(start, board));

        return possibleMoves;
    }
}
