package org.example.chesspressoserver.models.gamemodels;

import lombok.Getter;

@Getter
public class Gamestate {

    private TeamColor team;
    private Move move;
    private ChessPiece [][] board;
}
