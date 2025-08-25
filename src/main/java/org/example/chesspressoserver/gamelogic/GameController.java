package org.example.chesspressoserver.gamelogic;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.gamelogic.modles.MoveStandard;
import org.example.chesspressoserver.models.gamemodels.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class GameController {
    private Board board;
    @Setter
    private TeamColor aktiveTeam;


    //Check, ob die Figur eine direkte Verbindung zum König hat
    private boolean checkKingConnection(Position pos) {
        Position king = getBoard().getKingPosition(aktiveTeam);
        return king != null && hasClearLine(pos, king);
    }

    private List<Position> getKingConnectionPostion(Position pos) {
        List<Position> positions = new ArrayList<>();
        List<Position> attackingPostion =
                checkStateAktiveTeam(pos, getBoard().getPiece(pos.getX(), pos.getY()));

        if (attackingPostion.isEmpty()) {
            positions.add(new Position(-1, -1));
            return positions;
        } else if (attackingPostion.size() > 1) {
            return positions;
        } else {
            Position atk = attackingPostion.get(0);

            if (pos.getX() == atk.getX()) {
                // gleiche Spalte
                for (int i = Math.min(pos.getY(), atk.getY()); i <= Math.max(pos.getY(), atk.getY()); i++) {
                    positions.add(new Position(pos.getX(), i));
                }
            } else if (pos.getY() == atk.getY()) {
                // gleiche Zeile
                for (int i = Math.min(pos.getX(), atk.getX()); i <= Math.max(pos.getX(), atk.getX()); i++) {
                    positions.add(new Position(i, pos.getY()));
                }
            } else {
                // DIAGONALE
                int dx = (atk.getX() > pos.getX()) ? 1 : -1;
                int dy = (atk.getY() > pos.getY()) ? 1 : -1;

                // nur wenn wirklich auf derselben Diagonale
                if (Math.abs(atk.getX() - pos.getX()) == Math.abs(atk.getY() - pos.getY())) {
                    int x = pos.getX();
                    int y = pos.getY();
                    positions.add(new Position(x, y)); // Start inkl., wird unten wieder entfernt
                    while (x != atk.getX() || y != atk.getY()) {
                        x += dx;
                        y += dy;
                        positions.add(new Position(x, y));
                    }
                } else {
                    // nicht kollinear: nur Endpunkte zurückgeben
                    positions.add(new Position(pos.getX(), pos.getY()));
                    positions.add(new Position(atk.getX(), atk.getY()));
                }
            }

            // Startfeld entfernen (bewegende Figur)
            positions.removeIf(p -> p.getX() == pos.getX() && p.getY() == pos.getY());
            return positions;
        }
    }

    private List<Position> checkStateAktiveTeam(Position startPos, ChessPiece startPiece) {
        TeamColor enemyTeam = (aktiveTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        Position kingPosition = getBoard().getKingPosition(aktiveTeam);

        getBoard().removePiece(startPos.getX(),  startPos.getY());

        List<Position> attackingPiece = new ArrayList<>();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                ChessPiece piece = getBoard().getPiece(row, col);
                if (piece != null && piece.getColour() == enemyTeam) {
                    if (piece.getType() == PieceType.QUEEN ||
                            piece.getType() == PieceType.ROOK ||
                            piece.getType() == PieceType.BISHOP) {

                        Position pos = new Position(col, row);
                        List<Position> moves = piece.getMove().getPossibleMoves(pos);
                        if (moves.contains(kingPosition)) {
                            attackingPiece.add(pos);
                        }
                    }
                }
            }
        }

        getBoard().setPiece(startPos.getX(), startPos.getY(), startPiece);
        return attackingPiece;
    }

    //wird überprüft, ob ein Schach gesetzt wird
    private boolean checkStateEnemyTeam(Position pos) {
        TeamColor enemyTeam = (aktiveTeam == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
        Position kingPosition = getBoard().getKingPosition(enemyTeam);
        ChessPiece piece = getBoard().getPiece(pos.getX(), pos.getY());
        List<Position> moves = piece.getMove().getPossibleMoves(pos);
        if(moves.contains(kingPosition)) {
            return true;
        }

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                piece = getBoard().getPiece(row, col); // row,col
                if (piece != null && piece.getColour() == aktiveTeam) {
                    if (piece.getType() == PieceType.QUEEN ||
                            piece.getType() == PieceType.ROOK ||
                            piece.getType() == PieceType.BISHOP) {

                        Position newPos = new Position(col, row); // x=col, y=row
                        moves = piece.getMove().getPossibleMoves(newPos);
                        if (moves.contains(kingPosition)) {
                            return true;
                        }
                        moves.clear();
                    }
                }
            }
        }
        return false;
    }

    //Die Figur wird auf dem Board versetzt
    private void changePiecePosition(Position startPos, Position endPos) {
        int fromRow = startPos.getY();
        int fromCol = startPos.getX();
        int toRow = endPos.getY();
        int toCol = endPos.getX();

        ChessPiece piece = board.getPiece(fromRow, fromCol);
        board.removePiece(fromRow, fromCol);
        board.setPiece(toRow, toCol, piece);
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
}
