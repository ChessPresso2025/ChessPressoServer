package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveStandard {

    public List<Position> horizontalRight(Position pos, Board board){
        List<Position> moves = new ArrayList<>();
        int row = pos.getY();
        for (int col = pos.getX()+1; col < 8; col++) {
            if (board.checkEmpty(row, col)) {
                moves.add(new Position(col, row));
            } else {
                ChessPiece startPiece = board.getPiece(row, pos.getX());
                ChessPiece piece = board.getPiece(row, col);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(col, row));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> horizontalLeft(Position pos, Board board){
        List<Position> moves = new ArrayList<>();
        int row = pos.getY();
        for (int col = pos.getX()-1; col >= 0; col--) {
            if (board.checkEmpty(row, col)) {
                moves.add(new Position(col, row));
            } else {
                ChessPiece startPiece = board.getPiece(row, pos.getX());
                ChessPiece piece = board.getPiece(row, col);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(col, row));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> verticalUp(Position pos, Board board){
        List<Position> moves = new ArrayList<>();
        int col = pos.getX();
        for (int row = pos.getY()-1; row >= 0; row--) {
            if (board.checkEmpty(row, col)) {
                moves.add(new Position(col, row));
            } else {
                ChessPiece startPiece = board.getPiece(pos.getY(), col);
                ChessPiece piece = board.getPiece(row, col);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(col, row));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> verticalDown(Position pos, Board board){
        List<Position> moves = new ArrayList<>();
        int col = pos.getX();
        for (int row = pos.getY()+1; row < 8; row++) {
            if (board.checkEmpty(row, col)) {
                moves.add(new Position(col, row));
            } else {
                ChessPiece startPiece = board.getPiece(pos.getY(), col);
                ChessPiece piece = board.getPiece(row, col);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(col, row));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> diagonalUpRight(Position pos, Board board){
        List<Position> moves = new ArrayList<>();
        int x = pos.getX(), y = pos.getY();
        for (int i = 1; (x + i) < 8 && (y - i) >= 0; i++) { // up = row--
            int col = x + i, row = y - i;
            if (board.checkEmpty(row, col)) {
                moves.add(new Position(col, row));
            } else {
                ChessPiece startPiece = board.getPiece(y, x);
                ChessPiece piece = board.getPiece(row, col);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(col, row));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> diagonalUpLeft(Position pos, Board board){
        List<Position> moves = new ArrayList<>();
        int x = pos.getX(), y = pos.getY();
        for (int i = 1; (x - i) >= 0 && (y - i) >= 0; i++) {
            int col = x - i, row = y - i;
            if (board.checkEmpty(row, col)) {
                moves.add(new Position(col, row));
            } else {
                ChessPiece startPiece = board.getPiece(y, x);
                ChessPiece piece = board.getPiece(row, col);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(col, row));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> diagonalDownRight(Position pos, Board board){
        List<Position> moves = new ArrayList<>();
        int x = pos.getX(), y = pos.getY();
        for (int i = 1; (x + i) < 8 && (y + i) < 8; i++) {
            int col = x + i, row = y + i;
            if (board.checkEmpty(row, col)) {
                moves.add(new Position(col, row));
            } else {
                ChessPiece startPiece = board.getPiece(y, x);
                ChessPiece piece = board.getPiece(row, col);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(col, row));
                }
                break;
            }
        }
        return moves;
    }

    public List<Position> diagonalDownLeft(Position pos, Board board){
        List<Position> moves = new ArrayList<>();
        int x = pos.getX(), y = pos.getY();
        for (int i = 1; (x - i) >= 0 && (y + i) < 8; i++) {
            int col = x - i, row = y + i;
            if (board.checkEmpty(row, col)) {
                moves.add(new Position(col, row));
            } else {
                ChessPiece startPiece = board.getPiece(y, x);
                ChessPiece piece = board.getPiece(row, col);
                if (startPiece.getColour() != piece.getColour()) {
                    moves.add(new Position(col, row));
                }
                break;
            }
        }
        return moves;
    }
}
