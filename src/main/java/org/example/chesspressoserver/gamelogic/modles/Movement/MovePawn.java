package org.example.chesspressoserver.gamelogic.modles.Movement;

import lombok.Getter;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;
import org.example.chesspressoserver.models.gamemodels.TeamColor;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MovePawn extends PieceMove {

    private final TeamColor color;

    public MovePawn(TeamColor colour) {
        this.color = colour;
    }

    @Override
    public List<Position> getPossibleMoves(Position start, Board board) {
        List<Position> moves = new ArrayList<>(4);

        int x = start.getX(); // file (A..H) -> 0..7
        int y = start.getY(); // rank (1..8) -> 0..7

        // Richtung in y: Weiß nach oben (+1), Schwarz nach unten (-1)
        int dir = (color == TeamColor.WHITE) ? +1 : -1;

        // Startreihe (für double push / 2-step)
        int startRow = (color == TeamColor.WHITE) ? 1 : 6;

        // --- 1 Feld vor (quiet move) ---
        int y1 = y + dir;
        if (inBounds(x, y1) && board.checkEmpty(y1, x)) {
            moves.add(new Position(x, y1));

            // --- 2 Felder vor (nur von Startreihe, beide Felder leer) ---
            int y2 = y + 2 * dir;
            if (y == startRow && inBounds(x, y2) && board.checkEmpty(y2, x)) {
                moves.add(new Position(x, y2));
            }
        }

        // --- Diagonale Captures (nur wenn Ziel besetzt ist und Gegnerfarbe hat) ---
        int[] dxs = {-1, +1};
        ChessPiece self = board.getPiece(y, x);
        for (int dx : dxs) {
            int cx = x + dx;
            int cy = y + dir;
            if (!inBounds(cx, cy)) continue;

            if (!board.checkEmpty(cy, cx)) { // Feld ist besetzt?
                ChessPiece target = board.getPiece(cy, cx); // row=y, col=x
                if (target != null && self != null && target.getColour() != self.getColour()) {
                    moves.add(new Position(cx, cy));
                }
            }
        }
        return moves;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }
}
