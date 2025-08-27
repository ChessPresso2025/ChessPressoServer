package org.example.chesspressoserver.gamelogic.modles.Movement;

import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveKing extends PieceMove {
    @Override
    public List<Position> getPossibleMoves(Position start, Board board) {
        List<Position> moves = new ArrayList<>();

        int x = start.getX();
        int y = start.getY();

        // 8 Nachbarfelder
        int[][] directions = {
                { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
                { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 }
        };

        ChessPiece self = board.getPiece(y, x); // row=y, col=x

        for (int[] d : directions) {
            int nx = x + d[0];
            int ny = y + d[1];

            if (nx < 0 || nx >= 8 || ny < 0 || ny >= 8) continue;

            if (board.checkEmpty(ny, nx)) {
                moves.add(new Position(nx, ny));
            } else {
                ChessPiece tgt = board.getPiece(ny, nx); // row=y, col=x
                if (tgt != null && tgt.getColour() != self.getColour()) {
                    moves.add(new Position(nx, ny));
                }
            }
        }
        return moves;
    }
}
