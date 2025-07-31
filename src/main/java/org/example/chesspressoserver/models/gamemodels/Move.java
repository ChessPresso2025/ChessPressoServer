package org.example.chesspressoserver.models.gamemodels;

import lombok.Getter;

public class Move {

    @Getter
    private Position start;
    @Getter
    private Position end;
    @Getter
    private PieceType piece;
    @Getter
    private SpezialMove spezialMove;

    //constructor
    public Move(Position start, Position end, PieceType piece) {
        this.start = start;
        this.end = end;
        this.piece = piece;
    }
    public Move(Position start, Position end, PieceType piece, SpezialMove spezialMove) {
        this.start = start;
        this.end = end;
        this.piece = piece;
        this.spezialMove = spezialMove;
    }
}
