package org.example.chesspressoserver.models.gamemodels;

import lombok.Getter;
import org.example.chesspressoserver.gamelogic.modles.Movement.*;

@Getter
public class ChessPiece {

    private final PieceType type;
    private final TeamColor colour;
    private PieceMove move;

    //Construktor
    public ChessPiece(PieceType type, TeamColor colour) {
        this.type = type;
        this.colour = colour;
        this.move  = createMove(type, colour);
    }

    private static PieceMove createMove(PieceType type, TeamColor colour) {
        return switch (type) {
            case PAWN  -> new MovePawn(colour);
            case ROOK  -> new MoveRook();
            case KNIGHT-> new MoveKnight();
            case BISHOP-> new MoveBishop();
            case QUEEN -> new MoveQueen();
            case KING  -> new MoveKing();
        };
    }
}