package org.example.chesspressoserver.gamelogic.modles;

import lombok.Getter;
import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;
import org.example.chesspressoserver.models.gamemodels.TeamColor;

import java.util.ArrayList;
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
        List<Position> moves = new ArrayList<>(4);

        final int posX = start.getX();
        final int posY = start.getY();
        final ChessPiece startPiece = board.getPiece(posX, posY);

        final int dir       = (color == TeamColor.WHITE) ? +1 : -1;
        final int startRow  = (color == TeamColor.WHITE) ? 1  : 6;
        final int promoRow  = (color == TeamColor.WHITE) ? 7  : 0;

        // 1 Feld vor
        int r1 = posX + dir;
        if (inBounds(r1, posY) && board.checkEmpty(r1, posY)) {
            moves.add(new Position(r1, posY));

            // 2 Felder vor (nur vom Startrow, beide Felder leer)
            int r2 = posX + 2 * dir;
            if (posX == startRow && inBounds(r2, posY) && board.checkEmpty(r2, posY)) {
                moves.add(new Position(r2, posY));
            }
        }

        // Diagonale Captures
        int[] dc = {-1, +1};
        for (int dcol : dc) {
            int xc = posX + dir;
            int yc = posY + dcol;
            if (inBounds(xc, yc) && !board.checkEmpty(xc, yc)) {
                ChessPiece target = board.getPiece(xc, yc);
                if (target != null && startPiece != null && target.getColour() != startPiece.getColour()) {
                    moves.add(new Position(xc, yc));
                }
            }
        }
        return moves;
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}