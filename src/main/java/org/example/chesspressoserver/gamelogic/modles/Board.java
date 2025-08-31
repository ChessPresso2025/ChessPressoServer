package org.example.chesspressoserver.gamelogic.modles;

import org.example.chesspressoserver.models.gamemodels.ChessPiece;
import org.example.chesspressoserver.models.gamemodels.PieceType;
import org.example.chesspressoserver.models.gamemodels.Position;
import org.example.chesspressoserver.models.gamemodels.TeamColor;

public class Board {
    private ChessPiece[][] cells = new ChessPiece[8][8];

    public ChessPiece getPiece(int row, int col) {
        ChessPiece piece = cells[row][col];
        return piece;
    }

    public void setPiece(int row, int col, ChessPiece piece) {
        cells[row][col] = piece;
    }

    public void removePiece(int row, int col) {
        cells[row][col] = null;
    }

    public boolean checkEmpty(int row, int col) {
        return cells[row][col] == null;
    }

    public Position getKingPosition(TeamColor teamColor) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece p = cells[row][col];
                if (p != null
                        && p.getType() == PieceType.KING
                        && p.getColour() == teamColor) {
                    return new Position(col, row);
                }
            }
        }
        return null;
    }

    public void start() {
        ChessPiece W_P = new ChessPiece(PieceType.PAWN, TeamColor.WHITE);
        ChessPiece W_R = new ChessPiece(PieceType.ROOK, TeamColor.WHITE);
        ChessPiece W_N = new ChessPiece(PieceType.KNIGHT, TeamColor.WHITE);
        ChessPiece W_B = new ChessPiece(PieceType.BISHOP, TeamColor.WHITE);
        ChessPiece W_Q = new ChessPiece(PieceType.QUEEN, TeamColor.WHITE);
        ChessPiece W_K = new ChessPiece(PieceType.KING, TeamColor.WHITE);

        ChessPiece B_P = new ChessPiece(PieceType.PAWN, TeamColor.BLACK);
        ChessPiece B_R = new ChessPiece(PieceType.ROOK, TeamColor.BLACK);
        ChessPiece B_N = new ChessPiece(PieceType.KNIGHT, TeamColor.BLACK);
        ChessPiece B_B = new ChessPiece(PieceType.BISHOP, TeamColor.BLACK);
        ChessPiece B_Q = new ChessPiece(PieceType.QUEEN, TeamColor.BLACK);
        ChessPiece B_K = new ChessPiece(PieceType.KING, TeamColor.BLACK);

        cells = new ChessPiece[][]{
                {W_R, W_N, W_B, W_Q, W_K, W_B, W_N, W_R},
                {W_P, W_P, W_P, W_P, W_P, W_P, W_P, W_P},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {B_P, B_P, B_P, B_P, B_P, B_P, B_P, B_P},
                {B_R, B_N, B_B, B_Q, B_K, B_B, B_N, B_R}
        };
    }
}