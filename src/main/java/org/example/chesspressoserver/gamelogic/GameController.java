package org.example.chesspressoserver.gamelogic;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.gamelogic.modles.MoveStandard;
import org.example.chesspressoserver.models.gamemodels.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GameController {
    private Board board;
    @Setter
    private TeamColor aktiveTeam;
    @Setter
    private Move move;

    //Check, ob die Figur eine direkte Verbindung zum König hat
    private boolean checkKingConnection() {
        Position king = getBoard().getKingPosition(aktiveTeam);
        return king != null && hasClearLine(startPos(), king);
    }

    private boolean hasClearLine(Position from, Position to) {
        int dx = Integer.compare(to.getX(), from.getX());
        int dy = Integer.compare(to.getY(), from.getY());

        // nur Linien/Diagonalen zulassen
        boolean aligned =
                (from.getX() == to.getX()) ||
                        (from.getY() == to.getY()) ||
                        (Math.abs(to.getX() - from.getX()) == Math.abs(to.getY() - from.getY()));
        if (!aligned) return false;

        int x = from.getX() + dx;
        int y = from.getY() + dy;
        while (x != to.getX() || y != to.getY()) {
            if (!getBoard().checkEmpty(y, x)) return false;
            x += dx;
            y += dy;
        }
        return true;
    }

    //wird überprüft, ob ein Schach gesetzt wird
    private boolean checkAktiveTeam(){
        TeamColor enemyTeam = (aktiveTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        Position kingPosition = getBoard().getKingPosition(enemyTeam);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getBoard().getPiece(row, col); // row,col
                if (piece != null && piece.getColour() == aktiveTeam) {
                    if (piece.getType() == PieceType.QUEEN ||
                            piece.getType() == PieceType.ROOK ||
                            piece.getType() == PieceType.BISHOP) {

                        Position pos = new Position(col, row); // x=col, y=row  ✅
                        List<Position> moves = piece.getMove().getPossibleMoves(pos);
                        if (moves.contains(kingPosition)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean checkEnemyTeam(){
        TeamColor enemyTeam = (aktiveTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        Position kingPosition = getBoard().getKingPosition(aktiveTeam);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getBoard().getPiece(row, col);
                if (piece != null && piece.getColour() == enemyTeam) {
                    if (piece.getType() == PieceType.QUEEN ||
                            piece.getType() == PieceType.ROOK ||
                            piece.getType() == PieceType.BISHOP) {

                        Position pos = new Position(col, row); // ✅
                        List<Position> moves = piece.getMove().getPossibleMoves(pos);
                        if (moves.contains(kingPosition)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    //Die Figur wird auf dem Board versetzt
    private void changePiecePosition() {
        int fromRow = startPos().getY();
        int fromCol = startPos().getX();
        int toRow   = endPos().getY();
        int toCol   = endPos().getX();

        ChessPiece piece = board.getPiece(fromRow, fromCol);
        board.removePiece(fromRow, fromCol);
        board.setPiece(toRow, toCol, piece);
    }



    //Move Methoden
    public Position startPos(){
        return move.getStart();
    }
    public Position endPos(){
        return move.getEnd();
    }
    public PieceType pieceType(){
        return move.getPiece();
    }

}
