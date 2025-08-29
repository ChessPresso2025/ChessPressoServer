package org.example.chesspressoserver.gamelogic.modles.Movement;

import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveKnight extends PieceMove {
    @Override
    public List<Position> getPossibleMoves(Position start, Board board) {
        List<Position> possibleMoves = new ArrayList<>();

        int x = start.getX();
        int y = start.getY();
        ChessPiece startPiece = board.getPiece(y, x);

        // Alle 8 möglichen Springerzüge
        int[][] directions = {
                { 2, 1 },   // 2x rechts, 1x hoch
                { 2, -1 },  // 2x rechts, 1x runter
                { 1, 2 },   // 1x rechts, 2x hoch
                { -1, 2 },  // 1x links, 2x hoch
                { -2, 1 },  // 2x links, 1x hoch
                { -2, -1 }, // 2x links, 1x runter
                { 1, -2 },  // 1x rechts, 2x runter
                { -1, -2 }  // 1x links, 2x runter
        };

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];

            // Prüfe, ob der Zug innerhalb des Bretts liegt
            if (newX >= 0 && newX < 8 && newY >= 0 && newY < 8) {
                // Prüfe, ob das Zielfeld leer ist
                if (board.checkEmpty(newY, newX)) {
                    possibleMoves.add(new Position(newX, newY));
                } else {
                    // Prüfe, ob auf dem Zielfeld eine gegnerische Figur steht
                    ChessPiece targetPiece = board.getPiece(newY, newX);
                    if (targetPiece.getColour() != startPiece.getColour()) {
                        possibleMoves.add(new Position(newX, newY));
                    }
                }
            }
        }

        return possibleMoves;
    }
}
