package org.example.chesspressoserver.gamelogic.modles.Movement;

import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveBishop extends PieceMove{
    @Override
    public List<Position> getPossibleMoves(Position start,  Board board) {
        List<Position> possibleMoves = new ArrayList<>();

        possibleMoves.addAll(MoveStandard.diagonalUpRight(start, board));
        possibleMoves.addAll(MoveStandard.diagonalUpLeft(start, board));
        possibleMoves.addAll(MoveStandard.diagonalDownRight(start, board));
        possibleMoves.addAll(MoveStandard.diagonalDownLeft(start, board));

        return possibleMoves;
    }
}
