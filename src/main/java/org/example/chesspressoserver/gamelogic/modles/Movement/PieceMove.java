package org.example.chesspressoserver.gamelogic.modles.Movement;

import org.example.chesspressoserver.gamelogic.modles.Board;
import org.example.chesspressoserver.models.gamemodels.Position;

import java.util.List;

public abstract class PieceMove {

    public abstract List<Position> getPossibleMoves(Position pos, Board board);

}
