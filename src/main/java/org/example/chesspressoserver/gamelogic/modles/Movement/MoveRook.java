package org.example.chesspressoserver.gamelogic.modles.Movement;

import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveRook extends PieceMove{
    @Override
    public List<Position> getPossibleMoves(Position start, Board board) {
        List<Position> possibleMoves = new ArrayList<>();

        possibleMoves.addAll(MoveStandard.horizontalRight(start, board));
        possibleMoves.addAll(MoveStandard.horizontalLeft(start, board));
        possibleMoves.addAll(MoveStandard.verticalUp(start, board));
        possibleMoves.addAll(MoveStandard.verticalDown(start, board));

        return possibleMoves;
    }
}
