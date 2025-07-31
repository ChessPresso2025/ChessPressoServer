package org.example.chesspressoserver.models.gamemodels;

import lombok.Getter;

public class Gamestate {

    @Getter
    private TeamColor team;
    @Getter
    private Move move;
    @Getter
    private int[][] board;


}
