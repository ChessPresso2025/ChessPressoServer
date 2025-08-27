package org.example.chesspressoserver.gamelogic.modles.Movement;

import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveStandard {

    // ---------------- Horizontal ----------------

    public static List<Position> horizontalRight(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int y = pos.getY();
        for (int x = pos.getX() + 1; x < 8; x++) {
            if (board.checkEmpty(y, x)) {
                moves.add(new Position(x, y));
            } else {
                ChessPiece startPiece = board.getPiece(y, pos.getX());
                ChessPiece piece = board.getPiece(y, x);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(x, y));
                }
                break;
            }
        }
        return moves;
    }

    public static List<Position> horizontalLeft(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int y = pos.getY();
        for (int x = pos.getX() - 1; x >= 0; x--) {
            if (board.checkEmpty(y, x)) {
                moves.add(new Position(x, y));
            } else {
                ChessPiece startPiece = board.getPiece(y, pos.getX());
                ChessPiece piece = board.getPiece(y, x);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(x, y));
                }
                break;
            }
        }
        return moves;
    }

    // ---------------- Vertikal ----------------

    public static List<Position> verticalUp(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int x = pos.getX();
        for (int y = pos.getY() - 1; y >= 0; y--) {
            if (board.checkEmpty(y, x)) {
                moves.add(new Position(x, y));
            } else {
                ChessPiece startPiece = board.getPiece(pos.getY(), x);
                ChessPiece piece = board.getPiece(y, x);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(x, y));
                }
                break;
            }
        }
        return moves;
    }

    public static List<Position> verticalDown(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int x = pos.getX();
        for (int y = pos.getY() + 1; y < 8; y++) {
            if (board.checkEmpty(y, x)) {
                moves.add(new Position(x, y));
            } else {
                ChessPiece startPiece = board.getPiece(pos.getY(), x);
                ChessPiece piece = board.getPiece(y, x);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(x, y));
                }
                break;
            }
        }
        return moves;
    }

    // ---------------- Diagonal ----------------

    public static List<Position> diagonalUpRight(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int startX = pos.getX();
        int startY = pos.getY();

        for (int i = 1; (startX + i) < 8 && (startY - i) >= 0; i++) {
            int x = startX + i;
            int y = startY - i;
            if (board.checkEmpty(y, x)) {
                moves.add(new Position(x, y));
            } else {
                ChessPiece startPiece = board.getPiece(startY, startX);
                ChessPiece piece = board.getPiece(y, x);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(x, y));
                }
                break;
            }
        }
        return moves;
    }

    public static List<Position> diagonalUpLeft(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int startX = pos.getX();
        int startY = pos.getY();

        for (int i = 1; (startX - i) >= 0 && (startY - i) >= 0; i++) {
            int x = startX - i;
            int y = startY - i;
            if (board.checkEmpty(y, x)) {
                moves.add(new Position(x, y));
            } else {
                ChessPiece startPiece = board.getPiece(startY, startX);
                ChessPiece piece = board.getPiece(y, x);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(x, y));
                }
                break;
            }
        }
        return moves;
    }

    public static List<Position> diagonalDownRight(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int startX = pos.getX();
        int startY = pos.getY();

        for (int i = 1; (startX + i) < 8 && (startY + i) < 8; i++) {
            int x = startX + i;
            int y = startY + i;
            if (board.checkEmpty(y, x)) {
                moves.add(new Position(x, y));
            } else {
                ChessPiece startPiece = board.getPiece(startY, startX);
                ChessPiece piece = board.getPiece(y, x);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(x, y));
                }
                break;
            }
        }
        return moves;
    }

    public static List<Position> diagonalDownLeft(Position pos, Board board) {
        List<Position> moves = new ArrayList<>();
        int startX = pos.getX();
        int startY = pos.getY();

        for (int i = 1; (startX - i) >= 0 && (startY + i) < 8; i++) {
            int x = startX - i;
            int y = startY + i;
            if (board.checkEmpty(y, x)) {
                moves.add(new Position(x, y));
            } else {
                ChessPiece startPiece = board.getPiece(startY, startX);
                ChessPiece piece = board.getPiece(y, x);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(x, y));
                }
                break;
            }
        }
        return moves;
    }
}
