package org.example.chesspressoserver.models.gamemodels;

import lombok.Getter;

@Getter
public class ChessPiece {

    private final PieceType type;
    private final TeamColor color;

    //Construktor
    public ChessPiece(PieceType type, TeamColor color) {
        this.type = type;
        this.color = color;
    }
}
