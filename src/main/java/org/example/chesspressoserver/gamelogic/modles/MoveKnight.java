package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveKnight extends PieceMove {
    @Override
    public List<Position> getPossibleMoves(Position start) {
        List<Position> possibleMoves = new ArrayList<>();

        int x = start.getX();
        int y = start.getY();

        // Alle 8 Richtungen abchecken
        int[][] directions = {
                { 2 , 1 },  // 2x rechts hoch
                { 2 , -1 }, // 2x rechts runter
                { 1 , 2 },  // 2x hoch rechts
                { -1, 2 },  // 2x hoch links
                { -2, 1 },  // 2x links hoch
                { -2, -1 }, // 2x links runter
                { 1 , -2 }, // 2x runter rechts
                { -1, -2 }  // 2x runter links
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
