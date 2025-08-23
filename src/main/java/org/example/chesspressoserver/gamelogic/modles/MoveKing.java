package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveKing extends PieceMove {
    @Override
    public List<Position> getPossibleMoves(Position start) {
        List<Position> possibleMoves = new ArrayList<>();

        int x = start.getX();
        int y = start.getY();

        // Alle 8 Richtungen abchecken
        int[][] directions = {
                { 1, 0 },  // rechts
                { -1, 0 }, // links
                { 0, 1 },  // hoch
                { 0, -1 }, // runter
                { 1, 1 },  // diagonal rechts oben
                { 1, -1 }, // diagonal rechts unten
                { -1, 1 }, // diagonal links oben
                { -1, -1 } // diagonal links unten
        };

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];

            // Brettgrenzen checken (0–7)
            if (newX >= 0 && newX < 8 && newY >= 0 && newY < 8) {
                if (board.checkEmpty(newX, newY)) {
                    possibleMoves.add(new Position(newX, newY));
                } else {
                    // Gegnerische Figur darf geschlagen werden
                    ChessPiece startPiece = board.getPiece(x, y);
                    ChessPiece piece = board.getPiece(newX, newY);
                    if (startPiece.getColour() != piece.getColour()) {
                        possibleMoves.add(new Position(newX, newY));
                    }
                }
            }
        }

        return possibleMoves;
    }
}
