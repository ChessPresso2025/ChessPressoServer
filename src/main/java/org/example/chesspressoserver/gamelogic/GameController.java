package org.example.chesspressoserver.gamelogic;

import lombok.Getter;
import lombok.Setter;
import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.*;

@Getter
public class GameController {
    private Board board;
    @Setter
    private TeamColor aktiveTeam;
    @Setter
    private Move move;

    //Check, ob die Figur eine direkte Verbindung zum König hat
    private boolean checkKingConnection(){
        Position kingPosition = getBoard().getKingPosition(aktiveTeam);

        return true;
    };

    //Die Figur wird auf dem Board versetzt
    private void changePiecePosition() {
        ChessPiece piece = new ChessPiece(pieceType(), aktiveTeam);
        board.removePiece(startPos().getX(), startPos().getY());
        board.setPiece(endPos().getX(), endPos().getY(), piece);
    }


    //Movemethoden
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
