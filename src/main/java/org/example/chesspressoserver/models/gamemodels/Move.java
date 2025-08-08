package org.example.chesspressoserver.models.gamemodels;

import lombok.Getter;

@Getter
public class Move {

    private Position start;
    private Position end;
    private PieceType piece;
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
